// Dashboard administrativo do Thindesk
// Busca os dados em /api/dashboard e monta indicadores, gráficos e tabelas.
(function () {
    'use strict';

    const API = window.THINDESK_API || '/api/dashboard';
    const INTERVALO_ATUALIZACAO = 60 * 1000;
    const TAMANHO_PAGINA = 10;

    const $ = (id) => document.getElementById(id);
    const form = $('filtros');
    let pagina = 0;
    let timer = null;
    let ultimoPeriodo = null;
    let carregando = false;

    // ------------------------------------------------------------ utilitários

    const numero = (v) => (v == null ? '-' : Number(v).toLocaleString('pt-BR'));

    function horasTexto(h) {
        if (h == null) return '-';
        if (h < 24) return h.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + ' h';
        const dias = Math.round((h / 24) * 10) / 10;
        return dias.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + (dias === 1 ? ' dia' : ' dias');
    }

    function dataHora(iso) {
        if (!iso) return '-';
        const d = new Date(iso);
        return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    }

    function escapar(texto) {
        if (texto == null) return '';
        return String(texto).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }

    function isoData(d) {
        const p = (n) => String(n).padStart(2, '0');
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
    }

    function parametros(extra) {
        const p = new URLSearchParams();
        ['dataInicio', 'dataFim', 'status', 'tipo', 'tecnico'].forEach((campo) => {
            const v = $(campo).value;
            if (v) p.set(campo, v);
        });
        Object.entries(extra || {}).forEach(([k, v]) => p.set(k, v));
        return p.toString();
    }

    async function buscar(caminho, extra) {
        const resp = await fetch(API + caminho + '?' + parametros(extra), {
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        });
        if (resp.status === 401) {
            throw new Error('Sua sessão expirou. <a href="/login">Entre novamente</a>.');
        }
        if (!resp.ok) {
            let msg = 'Erro ' + resp.status + ' ao consultar a API.';
            try {
                const corpo = await resp.json();
                if (corpo.mensagem) msg = escapar(corpo.mensagem);
            } catch (e) { /* resposta sem JSON */ }
            throw new Error(msg);
        }
        return resp.json();
    }

    function mostrarErro(msg) {
        const el = $('mensagem');
        if (!msg) {
            el.classList.add('d-none');
            return;
        }
        el.innerHTML = msg;
        el.classList.remove('d-none');
    }

    // ------------------------------------------------------------ carregamento

    async function carregarOpcoes() {
        try {
            const op = await fetch(API + '/filtros', { credentials: 'same-origin' }).then((r) => {
                if (!r.ok) throw new Error();
                return r.json();
            });
            preencherSelect('status', op.status);
            preencherSelect('tipo', op.tipos);
            preencherSelect('tecnico', op.tecnicos);
        } catch (e) {
            // Sem as opções, os filtros de data continuam funcionando
        }
    }

    function preencherSelect(id, valores) {
        const sel = $(id);
        const atual = sel.value || new URLSearchParams(location.search).get(id) || '';
        sel.innerHTML = '<option value="">Todos</option>' +
            (valores || []).map((v) => '<option>' + escapar(v) + '</option>').join('');
        sel.value = atual;
    }

    async function carregarTudo() {
        if (carregando) return;
        carregando = true;
        $('btnAtualizar').disabled = true;
        try {
            const [resumo, periodo, status, tipos, tecnicos] = await Promise.all([
                buscar('/resumo'),
                buscar('/periodo', { agrupamento: $('agrupamento').value }),
                buscar('/status'),
                buscar('/tipos', { limite: 7 }),
                buscar('/tecnicos')
            ]);
            await carregarChamados();
            mostrarErro(null);
            desenharResumo(resumo);
            ultimoPeriodo = periodo;
            desenharPeriodo(periodo);
            desenharStatus(status);
            desenharTipos(tipos);
            desenharTecnicos(tecnicos);
            $('atualizadoEm').textContent = 'Atualizado às ' +
                new Date(resumo.atualizadoEm).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        } catch (e) {
            mostrarErro(e.message || 'Não foi possível carregar o dashboard.');
            $('atualizadoEm').textContent = 'Falha na última atualização';
        } finally {
            carregando = false;
            $('btnAtualizar').disabled = false;
        }
    }

    async function carregarChamados() {
        const dados = await buscar('/chamados', { pagina: pagina, tamanho: TAMANHO_PAGINA });
        desenharChamados(dados);
    }

    // ------------------------------------------------------------ RF01

    function desenharResumo(r) {
        $('ind-total').textContent = numero(r.total);
        $('ind-abertos').textContent = numero(r.abertos);
        $('ind-andamento').textContent = numero(r.emAndamento);
        $('ind-concluidos').textContent = numero(r.concluidos);
        $('ind-cancelados').textContent = numero(r.cancelados);
        $('ind-taxa').textContent = r.taxaResolucao == null ? '-' : r.taxaResolucao.toLocaleString('pt-BR') + '%';
        $('ind-tempo').textContent = horasTexto(r.tempoMedioResolucaoHoras);
        $('ind-prazo').textContent = numero(r.pendentesForaDoPrazo);
        $('ind-prazo-rotulo').textContent = 'Pendentes há mais de ' + r.prazoHoras + ' h';
    }

    // ------------------------------------------------------------ RF02

    function desenharPeriodo(p) {
        const alvo = $('grafico-periodo');
        $('periodo-total').textContent = numero(p.totalAbertos) + ' abertos e ' +
            numero(p.totalConcluidos) + ' concluídos no período';

        // tabela alternativa (acessibilidade)
        $('tabela-periodo').innerHTML = '<table class="table table-sm"><thead><tr><th>Período</th>' +
            '<th class="text-end">Abertos</th><th class="text-end">Concluídos</th></tr></thead><tbody>' +
            p.pontos.map((x) => '<tr><td>' + escapar(x.rotulo) + '</td><td class="text-end">' + x.abertos +
                '</td><td class="text-end">' + x.concluidos + '</td></tr>').join('') + '</tbody></table>';

        if (!p.pontos.length) {
            alvo.innerHTML = '<p class="text-muted">Nenhum chamado com data no período.</p>';
            return;
        }

        const largura = Math.max(alvo.clientWidth, 320);
        const altura = 240;
        const m = { topo: 12, dir: 80, base: 26, esq: 34 };
        const w = largura - m.esq - m.dir;
        const h = altura - m.topo - m.base;
        const n = p.pontos.length;
        const max = Math.max(1, ...p.pontos.map((x) => Math.max(x.abertos, x.concluidos)));
        const passo = escalaY(max);
        const topo = Math.ceil(max / passo) * passo;

        const x = (i) => m.esq + (n === 1 ? w / 2 : (i * w) / (n - 1));
        const y = (v) => m.topo + h - (v / topo) * h;

        let svg = '<svg width="' + largura + '" height="' + altura + '" role="img" aria-label="Gráfico de chamados abertos e concluídos por período">';

        // grade e eixo Y
        for (let v = 0; v <= topo; v += passo) {
            svg += '<line x1="' + m.esq + '" x2="' + (m.esq + w) + '" y1="' + y(v) + '" y2="' + y(v) +
                '" stroke="var(--grade)" stroke-width="1"/>';
            svg += '<text x="' + (m.esq - 6) + '" y="' + (y(v) + 4) + '" text-anchor="end">' + v + '</text>';
        }

        // rótulos do eixo X (no máximo ~10 para não sobrepor)
        const salto = Math.ceil(n / Math.max(2, Math.floor(w / 70)));
        p.pontos.forEach((pt, i) => {
            if (i % salto === 0 || i === n - 1) {
                svg += '<text x="' + x(i) + '" y="' + (altura - 6) + '" text-anchor="middle">' + escapar(pt.rotulo.replace('Sem. ', '')) + '</text>';
            }
        });

        const linha = (campo, cor) => {
            const d = p.pontos.map((pt, i) => (i ? 'L' : 'M') + x(i).toFixed(1) + ',' + y(pt[campo]).toFixed(1)).join(' ');
            let s = '<path d="' + d + '" fill="none" stroke="' + cor + '" stroke-width="2" stroke-linejoin="round"/>';
            if (n <= 40) {
                p.pontos.forEach((pt, i) => {
                    s += '<circle cx="' + x(i) + '" cy="' + y(pt[campo]) + '" r="3" fill="' + cor + '" stroke="#fff" stroke-width="1.5"/>';
                });
            }
            return s;
        };
        svg += linha('abertos', 'var(--cor-abertos)');
        svg += linha('concluidos', 'var(--cor-concluidos)');

        // rótulo direto no fim de cada linha
        const ultimo = p.pontos[n - 1];
        let yA = y(ultimo.abertos);
        let yC = y(ultimo.concluidos);
        if (Math.abs(yA - yC) < 13) {
            if (yA <= yC) { yC = yA + 13; } else { yA = yC + 13; }
        }
        svg += '<text x="' + (x(n - 1) + 8) + '" y="' + (yA + 4) + '" style="fill:#212529">Abertos</text>';
        svg += '<text x="' + (x(n - 1) + 8) + '" y="' + (yC + 4) + '" style="fill:#212529">Concluídos</text>';

        // linha guia do hover
        svg += '<line id="guia" x1="0" x2="0" y1="' + m.topo + '" y2="' + (m.topo + h) + '" stroke="#adb5bd" stroke-width="1" visibility="hidden"/>';
        svg += '<rect id="area-hover" x="' + m.esq + '" y="' + m.topo + '" width="' + w + '" height="' + h + '" fill="transparent"/>';
        svg += '</svg>';

        alvo.innerHTML = '<div class="legenda mb-1"><i style="background:var(--cor-abertos)"></i>Abertos' +
            '<i style="background:var(--cor-concluidos)"></i>Concluídos</div>' + svg;

        const area = alvo.querySelector('#area-hover');
        const guia = alvo.querySelector('#guia');
        const tip = $('tooltip');
        area.addEventListener('mousemove', (ev) => {
            const r = area.getBoundingClientRect();
            const rel = (ev.clientX - r.left) / r.width;
            const i = Math.min(n - 1, Math.max(0, Math.round(rel * (n - 1))));
            const pt = p.pontos[i];
            guia.setAttribute('x1', x(i));
            guia.setAttribute('x2', x(i));
            guia.setAttribute('visibility', 'visible');
            tip.innerHTML = '<strong>' + escapar(pt.rotulo) + '</strong><br>Abertos: ' + pt.abertos + '<br>Concluídos: ' + pt.concluidos;
            tip.style.left = (ev.clientX + 14) + 'px';
            tip.style.top = (ev.clientY - 10) + 'px';
            tip.classList.remove('d-none');
        });
        area.addEventListener('mouseleave', () => {
            guia.setAttribute('visibility', 'hidden');
            tip.classList.add('d-none');
        });
    }

    function escalaY(max) {
        const bruto = max / 4;
        const base = Math.pow(10, Math.floor(Math.log10(bruto)));
        const opcoes = [1, 2, 5, 10].map((f) => f * base);
        return Math.max(1, opcoes.find((o) => o >= bruto));
    }

    // ------------------------------------------------------------ RF03

    function desenharStatus(d) {
        const max = Math.max(1, ...d.itens.map((i) => i.quantidade));
        $('grafico-status').innerHTML = d.itens.map((i) =>
            '<div class="status-linha" title="' + escapar(i.nome) + ': ' + i.quantidade + ' (' + i.percentual + '%)">' +
            '<span>' + escapar(i.nome) + '</span>' +
            '<div class="barra-fundo"><div class="barra" style="width:' + (100 * i.quantidade / max) + '%"></div></div>' +
            '<span class="text-end"><strong>' + numero(i.quantidade) + '</strong> <small class="text-muted">' +
            i.percentual.toLocaleString('pt-BR') + '%</small></span></div>'
        ).join('') + '<p class="text-muted small mt-2 mb-0">Total: ' + numero(d.total) + ' chamados</p>';
    }

    // ------------------------------------------------------------ RF04

    function desenharTipos(lista) {
        if (!lista.length) {
            $('tabela-tipos').innerHTML = '<tr><td colspan="5" class="text-muted">Nenhum chamado encontrado.</td></tr>';
            return;
        }
        const max = Math.max(1, ...lista.map((t) => t.total));
        $('tabela-tipos').innerHTML = lista.map((t) =>
            '<tr><td>' + t.posicao + 'º</td><td>' + escapar(t.tipo) + '</td>' +
            '<td class="text-end">' + numero(t.total) + ' <small class="text-muted">(' + t.percentual.toLocaleString('pt-BR') + '%)</small></td>' +
            '<td class="text-end">' + numero(t.concluidos) + '</td>' +
            '<td><div class="barra-fundo"><div class="barra" style="width:' + (100 * t.total / max) + '%"></div></div></td></tr>'
        ).join('');
    }

    function desenharTecnicos(lista) {
        $('tabela-tecnicos').innerHTML = lista.length ? lista.map((t) =>
            '<tr><td>' + escapar(t.tecnico) + '</td><td class="text-end">' + numero(t.atribuidos) + '</td>' +
            '<td class="text-end">' + numero(t.concluidos) + '</td><td class="text-end">' + numero(t.pendentes) + '</td>' +
            '<td class="text-end">' + horasTexto(t.tempoMedioResolucaoHoras) + '</td></tr>'
        ).join('') : '<tr><td colspan="5" class="text-muted">Nenhum chamado encontrado.</td></tr>';
    }

    function desenharChamados(p) {
        $('tabela-chamados').innerHTML = p.itens.length ? p.itens.map((c) =>
            '<tr><td class="text-nowrap">' + dataHora(c.dataAbertura) + '</td>' +
            '<td>' + escapar(c.descricao) + '</td><td>' + escapar(c.tipo) + '</td>' +
            '<td class="text-nowrap">' + escapar(c.status) + (c.foraDoPrazo ? ' <span class="atrasado">atrasado</span>' : '') + '</td>' +
            '<td>' + escapar(c.tecnico) + '</td><td>' + escapar(c.prioridade || '-') + '</td>' +
            '<td>' + escapar(c.consideracoesTecnicas || '-') + '</td>' +
            '<td class="text-end text-nowrap">' + horasTexto(c.horasEmAberto) + '</td></tr>'
        ).join('') : '<tr><td colspan="8" class="text-muted">Nenhum chamado encontrado.</td></tr>';

        const totalPag = Math.max(1, p.totalPaginas);
        $('paginaInfo').textContent = 'Página ' + (p.pagina + 1) + ' de ' + totalPag + ' (' + numero(p.totalItens) + ' chamados)';
        $('paginaAnterior').disabled = p.pagina <= 0;
        $('paginaProxima').disabled = p.pagina + 1 >= totalPag;
    }

    // ------------------------------------------------------------ eventos

    function salvarNaUrl() {
        history.replaceState(null, '', location.pathname + '?' + parametros({ agrupamento: $('agrupamento').value }));
    }

    function aplicar() {
        pagina = 0;
        salvarNaUrl();
        carregarTudo();
    }

    function programarAtualizacao() {
        clearInterval(timer);
        if ($('autoAtualizar').checked) {
            timer = setInterval(carregarTudo, INTERVALO_ATUALIZACAO);
        }
    }

    function iniciar() {
        // Filtros vindos da URL; se não houver, últimos 30 dias
        const url = new URLSearchParams(location.search);
        const hoje = new Date();
        const inicio = new Date();
        inicio.setDate(hoje.getDate() - 29);
        $('dataInicio').value = url.get('dataInicio') ?? isoData(inicio);
        $('dataFim').value = url.get('dataFim') ?? isoData(hoje);
        if (url.get('agrupamento')) $('agrupamento').value = url.get('agrupamento');

        form.addEventListener('submit', (ev) => {
            ev.preventDefault();
            aplicar();
        });
        $('btnLimpar').addEventListener('click', () => {
            ['status', 'tipo', 'tecnico'].forEach((id) => { $(id).value = ''; });
            $('dataInicio').value = isoData(inicio);
            $('dataFim').value = isoData(hoje);
            aplicar();
        });
        $('agrupamento').addEventListener('change', aplicar);
        $('btnAtualizar').addEventListener('click', carregarTudo);
        $('autoAtualizar').addEventListener('change', programarAtualizacao);
        $('paginaAnterior').addEventListener('click', () => { pagina--; carregarChamados().catch((e) => mostrarErro(e.message)); });
        $('paginaProxima').addEventListener('click', () => { pagina++; carregarChamados().catch((e) => mostrarErro(e.message)); });
        $('alternarTabelaPeriodo').addEventListener('click', (ev) => {
            ev.preventDefault();
            const tabela = $('tabela-periodo');
            const mostrar = tabela.classList.contains('d-none');
            tabela.classList.toggle('d-none', !mostrar);
            $('grafico-periodo').classList.toggle('d-none', mostrar);
            ev.target.textContent = mostrar ? 'ver gráfico' : 'ver tabela';
        });

        let espera;
        window.addEventListener('resize', () => {
            clearTimeout(espera);
            espera = setTimeout(() => { if (ultimoPeriodo) desenharPeriodo(ultimoPeriodo); }, 150);
        });

        carregarOpcoes().then(carregarTudo);
        programarAtualizacao();
    }

    iniciar();
})();
