<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Криптовалюты</title>
    <style>
        body { font-family: Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }
        h1 { margin-bottom: 10px; }
        .controls { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
        table { width: 100%; border-collapse: collapse; background: #fff; }
        th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
        thead { background: #f0f0f0; }
        .positive { color: #1f8b4c; }
        .negative { color: #c0392b; }
        .chart { margin-top: 24px; background: #fff; padding: 16px; }
        .chart-container { height: 300px; }
        select { padding: 6px; }
    </style>
</head>
<body>
<h1>Топ-10 криптовалют по капитализации</h1>
<#assign ctxVal = ctx!"" >
<div class="controls">
    <label for="currency-select">Валюта отображения:</label>
    <select id="currency-select">
        <#list currencies as cur>
            <option value="${cur}" <#if cur == currency>selected</#if>>${cur?upper_case}</option>
        </#list>
    </select>
    <span class="note">Данные обновляются автоматически каждые 60 секунд</span>
    <span id="status"></span>
    <span id="error"></span>
    <span id="updated"></span>
</div>
<table id="coins-table">
    <thead>
    <tr>
        <th>Название</th>
        <th>Символ</th>
        <th>Цена</th>
        <th>Изм. 24ч</th>
        <th>Капитализация</th>
    </tr>
    </thead>
    <tbody>
    <#list coins![] as coin>
        <tr data-coin-id="${coin.id!''}">
            <td>${coin.name!''}</td>
            <td>${(coin.symbol!'')?upper_case}</td>
            <td class="price-cell">${(coin.currentPrice!0)?string["#,##0.00"]}</td>
            <td class="${((coin.priceChangePercentage24h!0) >= 0)?then('positive','negative')}">
                ${(coin.priceChangePercentage24h!0)?string["0.00"]}%
            </td>
            <td class="price-cell">${(coin.marketCap!0)?string["#,##0"]}</td>
        </tr>
    </#list>
    </tbody>
</table>
<div class="chart">
    <h3 id="chart-title">7 дней: <#if coins?has_content>${coins?first.name}<#else>Нет данных</#if></h3>
    <div class="chart-container">
        <canvas id="priceChart"></canvas>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script>
(function() {
    const basePath = '${ctxVal}';
    const tableBody = document.querySelector('#coins-table tbody');
    const currencySelect = document.getElementById('currency-select');
    const chartTitle = document.getElementById('chart-title');
    const updated = document.getElementById('updated');
    const errorEl = document.getElementById('error');
    const status = document.getElementById('status');
    let chart = null;
    let updateInterval = null;

    function formatPrice(value) {
        return new Intl.NumberFormat('ru-RU', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(Number(value) || 0);
    }

    function formatMarketCap(value) {
        return new Intl.NumberFormat('ru-RU').format(Math.round(Number(value) || 0));
    }

    function getSparklineData(coin) {
        if (!coin) return [];
        if (Array.isArray(coin.sparkline) && coin.sparkline.length > 0) {
            return coin.sparkline;
        }
        if (coin.sparklineIn7d) {
            if (Array.isArray(coin.sparklineIn7d.prices)) {
                return coin.sparklineIn7d.prices;
            }
            if (Array.isArray(coin.sparklineIn7d.price)) {
                return coin.sparklineIn7d.price;
            }
        }
        if (coin.sparkline_in_7d) {
            if (Array.isArray(coin.sparkline_in_7d.prices)) {
                return coin.sparkline_in_7d.prices;
            }
            if (Array.isArray(coin.sparkline_in_7d.price)) {
                return coin.sparkline_in_7d.price;
            }
        }
        return [];
    }

    function updateChart(coin) {
        if (!coin) return;
        
        const sparkline = getSparklineData(coin);
        if (sparkline.length === 0) {
            if (chart) {
                chart.destroy();
                chart = null;
            }
            if (chartTitle) {
                chartTitle.textContent = '7 дней: ' + coin.name + ' (нет данных)';
            }
            return;
        }

        const canvas = document.getElementById('priceChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const chartData = {
            labels: sparkline.map((_, i) => ''),
            datasets: [{
                label: coin.name,
                data: sparkline,
                borderColor: '#2c7be5',
                backgroundColor: 'rgba(44,123,229,0.1)',
                fill: true,
                tension: 0.4,
                pointRadius: 0,
                borderWidth: 2
            }]
        };

        const chartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { enabled: true }
            },
            scales: {
                x: { display: false, grid: { display: false } },
                y: {
                    display: true,
                    beginAtZero: false,
                    grid: { color: 'rgba(0,0,0,0.1)' }
                }
            }
        };

        if (chart) {
            chart.data = chartData;
            chart.update();
        } else {
            chart = new Chart(ctx, {
                type: 'line',
                data: chartData,
                options: chartOptions
            });
        }

        if (chartTitle) {
            chartTitle.textContent = '7 дней: ' + coin.name;
        }
    }

    function renderTable(data) {
        if (!tableBody) return;
        
        tableBody.innerHTML = '';
        
        if (!data || data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 20px; color: #666;">Нет данных для отображения</td></tr>';
            return;
        }

        data.forEach((coin) => {
            const row = document.createElement('tr');
            row.dataset.coinId = coin.id;
            
            const price = Number(coin.currentPrice || coin.current_price || 0);
            const change = Number(coin.priceChangePercentage24h || coin.price_change_percentage_24h || 0);
            const marketCap = Number(coin.marketCap || coin.market_cap || 0);
            const changeClass = change >= 0 ? 'positive' : 'negative';
            
            row.innerHTML = 
                '<td>' + (coin.name || '') + '</td>' +
                '<td>' + (coin.symbol || '').toUpperCase() + '</td>' +
                '<td class="price-cell">' + formatPrice(price) + '</td>' +
                '<td class="' + changeClass + '">' + change.toFixed(2) + '%</td>' +
                '<td class="price-cell">' + formatMarketCap(marketCap) + '</td>';
            
            row.addEventListener('click', () => updateChart(coin));
            tableBody.appendChild(row);
        });
    }

    async function loadData() {
        if (errorEl) errorEl.textContent = '';
        if (status) status.textContent = 'Загрузка...';
        
        try {
            const currency = currencySelect ? currencySelect.value : 'usd';
            
            let apiPath = '/api/coins';
            if (basePath && basePath.trim() !== '') {
                const bp = basePath.trim();
                apiPath = (bp.startsWith('/') ? bp : '/' + bp) + 
                         (bp.endsWith('/') ? 'api/coins' : '/api/coins');
            }
            
            const url = apiPath + '?currency=' + encodeURIComponent(currency) + '&_=' + Date.now();
            
            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                cache: 'no-store'
            });
            
            if (!response.ok) {
                throw new Error('Ошибка загрузки: HTTP ' + response.status);
            }
            
            const data = await response.json();
            
            if (!Array.isArray(data) || data.length === 0) {
                throw new Error('Нет данных');
            }
            
            renderTable(data);
            
            if (data.length > 0) {
                updateChart(data[0]);
            }
            
            if (updated) {
                updated.textContent = 'Обновлено: ' + new Date().toLocaleTimeString('ru-RU');
            }
            if (status) status.textContent = '';
            
        } catch (e) {
            if (errorEl) {
                errorEl.textContent = 'Ошибка: ' + (e.message || 'неизвестная ошибка');
            }
            if (status) status.textContent = '';
        }
    }

    function init() {
        if (typeof Chart === 'undefined') {
            if (errorEl) {
                errorEl.textContent = 'Ошибка: Chart.js не загружен';
            }
            return;
        }

        if (currencySelect) {
            currencySelect.addEventListener('change', () => {
                loadData();
                const params = new URLSearchParams(location.search);
                params.set('currency', currencySelect.value);
                history.replaceState({}, '', location.pathname + '?' + params.toString());
            });
        }

        loadData();
        
        if (updateInterval) {
            clearInterval(updateInterval);
        }
        updateInterval = setInterval(loadData, 60000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
</script>
</body>
</html>

