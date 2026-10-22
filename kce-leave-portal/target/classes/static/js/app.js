// Ripple effect on every button
document.addEventListener('click', function (e) {
  var btn = e.target.closest('.btn');
  if (!btn) return;
  var r = btn.getBoundingClientRect(), s = Math.max(r.width, r.height);
  var span = document.createElement('span');
  span.className = 'ripple';
  span.style.width = span.style.height = s + 'px';
  span.style.left = (e.clientX - r.left - s / 2) + 'px';
  span.style.top = (e.clientY - r.top - s / 2) + 'px';
  btn.appendChild(span);
  setTimeout(function () { span.remove(); }, 600);
});
// Date inputs: no past dates, end date follows start date
document.addEventListener('DOMContentLoaded', function () {
  var from = document.getElementById('fromDate'), to = document.getElementById('toDate');
  if (!from || !to) return;
  var today = new Date().toISOString().split('T')[0];
  from.min = today; to.min = today;
  from.addEventListener('change', function () { to.min = from.value || today; if (to.value && to.value < from.value) to.value = from.value; });
});
