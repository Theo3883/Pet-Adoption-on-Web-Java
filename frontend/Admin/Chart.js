/**
 * Simple bar chart component for displaying activity data
 */
class ActivityChart {

    constructor(containerId, title, color, data) {
        this.container = document.getElementById(containerId);
        this.title = title;
        this.color = color;
        this.data = data;
        
        // Find maximum value for scaling
        const counts = this.data.map(item => item.count);
        const maxCount = counts.length > 0 ? Math.max(...counts) : 0;
       
        this.maxValue = Math.max(5, this.roundUpToNice(maxCount));
        
        this.render();
    }
    
    roundUpToNice(num) {
        if (num <= 5) return 5;
        if (num <= 10) return 10;
        
        const magnitude = Math.pow(10, Math.floor(Math.log10(num)));
        const normalized = num / magnitude;
        
        if (normalized <= 5) {
            return Math.ceil(normalized) * magnitude;
        } else {
            return Math.ceil(normalized / 5) * 5 * magnitude;
        }
    }
    
    static formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric'
        });
    }
    
    render() {
        
        if (!this.container) {
            console.error(`Chart container not found`);
            return;
        }
        
        
        this.container.innerHTML = '';
        
        
        const chartEl = document.createElement('div');
        chartEl.className = 'simple-chart';
        
       
        const titleEl = document.createElement('h3');
        titleEl.className = 'chart-title';
        titleEl.textContent = this.title;
        chartEl.appendChild(titleEl);
        
        
        const subtitleEl = document.createElement('div');
        subtitleEl.className = 'chart-subtitle';
        subtitleEl.textContent = 'Last 7 days';
        chartEl.appendChild(subtitleEl);
        
        
        const chartAreaEl = document.createElement('div');
        chartAreaEl.className = 'chart-area';
        
        
        const yAxisEl = document.createElement('div');
        yAxisEl.className = 'y-axis';
        
        
        const yLabels = [this.maxValue, Math.round(this.maxValue/2), 0];
        yLabels.forEach(value => {
            const labelEl = document.createElement('div');
            labelEl.className = 'y-label';
            labelEl.textContent = value;
            yAxisEl.appendChild(labelEl);
        });
        
        chartAreaEl.appendChild(yAxisEl);
        
        
        const chartDisplayEl = document.createElement('div');
        chartDisplayEl.className = 'chart-display';
        
      
        const gridEl = document.createElement('div');
        gridEl.className = 'grid-lines';
        
       
        for (let i = 0; i < 3; i++) {
            const lineEl = document.createElement('div');
            lineEl.className = 'grid-line';
            gridEl.appendChild(lineEl);
        }
        
        chartDisplayEl.appendChild(gridEl);
        
       
        const barsContainerEl = document.createElement('div');
        barsContainerEl.className = 'bars-container';
        
        
        this.data.forEach(item => {
            // Calculate height percentage
            const heightPercent = this.maxValue > 0 ? 
                (item.count / this.maxValue) * 100 : 0;
            
            // Create bar wrapper
            const barWrapperEl = document.createElement('div');
            barWrapperEl.className = 'bar-wrapper';
            
            // Create actual bar
            const barEl = document.createElement('div');
            barEl.className = 'bar';
            barEl.style.height = `${heightPercent}%`;
            barEl.style.backgroundColor = this.color;
            
            // Add count label if there's a value
            if (item.count > 0) {
                const countEl = document.createElement('div');
                countEl.className = 'bar-count';
                countEl.textContent = item.count;
                barEl.appendChild(countEl);
            }
            
            barWrapperEl.appendChild(barEl);
            barsContainerEl.appendChild(barWrapperEl);
        });
        
        chartDisplayEl.appendChild(barsContainerEl);
        chartAreaEl.appendChild(chartDisplayEl);
        
        // Create x-axis
        const xAxisEl = document.createElement('div');
        xAxisEl.className = 'x-axis';
        
        // Add x-axis labels
        this.data.forEach(item => {
            const labelEl = document.createElement('div');
            labelEl.className = 'x-label';
            labelEl.textContent = ActivityChart.formatDate(item.date);
            xAxisEl.appendChild(labelEl);
        });
        
        chartAreaEl.appendChild(xAxisEl);
        chartEl.appendChild(chartAreaEl);
        
       
        this.container.appendChild(chartEl);
        
        console.log('Chart rendered with data:', this.data);
    }
    

    updateData(newData) {
        this.data = newData;
        const counts = this.data.map(item => item.count);
        const maxCount = counts.length > 0 ? Math.max(...counts) : 0;
        this.maxValue = Math.max(5, this.roundUpToNice(maxCount));
        this.render();
    }
}


function processDataForChart(data, type) {
    // Get last 7 days
    const result = [];
    const today = new Date();
    
    // Create entries for each of the last 7 days with zero counts
    for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateString = date.toISOString().split('T')[0]; // YYYY-MM-DD format
        result.push({
            date: dateString,
            count: 0
        });
    }
    
    
    if (!data || data.length === 0) {
        return result;
    }
    
    if (type === 'users') {
       
        data.forEach(user => {
            if (!user.CREATEDAT) return;
            
            const createdDate = new Date(user.CREATEDAT);
            const dateString = createdDate.toISOString().split('T')[0];
            
            const dayRecord = result.find(day => day.date === dateString);
            if (dayRecord) {
                dayRecord.count++;
            }
        });
    } else if (type === 'pets') {
       
        data.forEach(animal => {
            if (!animal.CREATEDAT) return;
            
            const createdDate = new Date(animal.CREATEDAT);
            const dateString = createdDate.toISOString().split('T')[0];
            
            const dayRecord = result.find(day => day.date === dateString);
            if (dayRecord) {
                dayRecord.count++;
            }
        });
    }
    
    return result;
}


function initCharts(dashboardData) {
    if (!dashboardData) {
        console.error('No dashboard data available');
        return;
    }
    
    console.log('Initializing charts with data:', dashboardData);
    
  
    const userData = processDataForChart(dashboardData.users || [], 'users');
    const petsData = processDataForChart(dashboardData.animals || [], 'pets');
    
   
    new ActivityChart('users-chart', 'New Users', '#3498db', userData);
    new ActivityChart('pets-chart', 'New Pets', '#e74c3c', petsData);
}

window.chartModule = {
    ActivityChart,
    processDataForChart,
    initCharts
};