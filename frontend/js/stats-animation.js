// Stats Counter Animation Module
// Add to main.js or include separately

function animateStats() {
  const statsSection = document.querySelector('.statistics');
  if (!statsSection) return;

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        animateCounters();
        observer.disconnect(); // One-time animation
      }
    });
  }, { threshold: 0.3 });

  observer.observe(statsSection);

  function animateCounters() {
    const numbers = document.querySelectorAll('.stat-number');
    numbers.forEach(num => {
      const target = parseInt(num.dataset.target);
      const hasDollar = num.textContent.includes('$');
      let current = 0;
      const increment = target / 100;
      const duration = 2000; // 2 seconds
      const startTime = performance.now();

      function updateCounter(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // Easing function (easeOutQuad)
        const easeProgress = 1 - Math.pow(1 - progress, 2);
        current = Math.floor(easeProgress * target);

        // Format number
        let displayValue = hasDollar ? '$' + current.toLocaleString() : current.toLocaleString();
        num.textContent = displayValue;

        if (progress < 1) {
          requestAnimationFrame(updateCounter);
        }
      }

      requestAnimationFrame(updateCounter);
    });
  }
}

// Export for main.js
window.animateStats = animateStats;

