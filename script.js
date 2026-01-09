document.addEventListener('DOMContentLoaded',()=>{
  const medicines = [
    'Paracetamol 500mg','Amoxicillin 250mg','Insulin Glargine 100U/ml','Metformin 500mg','Aspirin 75mg','Omeprazole 20mg','Levothyroxine 50mcg','Atorvastatin 10mg'
  ];

  // Register page logic
  const donorRadio = document.getElementById('donorRadio');
  const recipientRadio = document.getElementById('recipientRadio');
  const donorFields = document.getElementById('donorFields');
  const recipientFields = document.getElementById('recipientFields');
  const regForm = document.getElementById('registrationForm');
  const message = document.getElementById('message');

  function toggleConditional(){
    if(donorRadio && donorRadio.checked){ donorFields.style.display='block'; recipientFields.style.display='none'; }
    else if(recipientRadio && recipientRadio.checked){ donorFields.style.display='none'; recipientFields.style.display='block'; }
    else { donorFields.style.display='none'; recipientFields.style.display='none'; }
  }
  if(donorRadio) donorRadio.addEventListener('change',toggleConditional);
  if(recipientRadio) recipientRadio.addEventListener('change',toggleConditional);
  toggleConditional();

  // Autocomplete
  const medInput = document.getElementById('medicine');
  if(medInput){
    const wrapper = document.getElementById('autocomplete-list');
    medInput.addEventListener('input',()=>{
      const q = medInput.value.trim().toLowerCase();
      wrapper.innerHTML='';
      if(!q) return;
      const matches = medicines.filter(m=>m.toLowerCase().includes(q)).slice(0,8);
      if(matches.length){
        const container = document.createElement('div'); container.className='autocomplete-suggestions';
        matches.forEach(s=>{
          const d = document.createElement('div'); d.textContent=s; d.addEventListener('click',()=>{ medInput.value=s; wrapper.innerHTML=''; });
          container.appendChild(d);
        });
        wrapper.appendChild(container);
      }
    });
    document.addEventListener('click',e=>{ if(!wrapper.contains(e.target) && e.target!==medInput) wrapper.innerHTML=''; });
  }

  // Persist registrations to localStorage (simple simulation)
  if(regForm){
    regForm.addEventListener('submit',e=>{
      e.preventDefault();
      const fd = new FormData(regForm);
      const user = Object.fromEntries(fd.entries());
      user.quantity = parseInt(user.quantity||0);
      user.latitude = parseFloat(user.latitude||0);
      user.longitude = parseFloat(user.longitude||0);
      user.type = user.userType||'recipient';
      user.id = Date.now();
      user.status = 'PENDING';
      const all = JSON.parse(localStorage.getItem('medswap_users')||'[]');
      all.push(user);
      localStorage.setItem('medswap_users',JSON.stringify(all));
      if(message){ message.style.display='block'; message.textContent='Registration saved locally. Open Users to view.'; message.className='message alert alert-success'; }
      regForm.reset(); toggleConditional();
    });
  }

  // View users page: populate tables
  function renderUsers(){
    const all = JSON.parse(localStorage.getItem('medswap_users')||'[]');
    const donorsTbody = document.querySelector('#donors tbody');
    const recipientsTbody = document.querySelector('#recipients tbody');
    if(donorsTbody) donorsTbody.innerHTML='';
    if(recipientsTbody) recipientsTbody.innerHTML='';
    all.forEach(u=>{
      const tr = document.createElement('tr');
      const avatar = (u.name||'').trim().charAt(0).toUpperCase()||'?';
      const loc = (u.latitude && u.longitude)? `${u.latitude.toFixed(4)}°, ${u.longitude.toFixed(4)}°` : '';
      if(u.type==='donor'){
        tr.innerHTML = `<td><div class="user-cell"><div class="user-avatar">${avatar}</div><span>${u.name}</span></div></td><td>${u.contact||''}</td><td>${u.medicine||''}</td><td>${u.quantity||''}</td><td>${u.expiry||''}</td><td>${loc}</td><td><span class="status-badge status-pending">${u.status}</span></td>`;
        donorsTbody && donorsTbody.appendChild(tr);
      } else {
        tr.innerHTML = `<td><div class="user-cell"><div class="user-avatar">${avatar}</div><span>${u.name}</span></div></td><td>${u.contact||''}</td><td>${u.medicine||''}</td><td>${u.quantity||''}</td><td>${u.urgency?(''+u.urgency):''}</td><td>${loc}</td><td><span class="status-badge status-pending">${u.status}</span></td>`;
        recipientsTbody && recipientsTbody.appendChild(tr);
      }
    });
  }
  renderUsers();

  // Tabs and search in users view
  const tabBtns = document.querySelectorAll('.tab-btn');
  tabBtns.forEach(b=>b.addEventListener('click',()=>{
    tabBtns.forEach(x=>x.classList.remove('active'));
    b.classList.add('active');
    document.querySelectorAll('.tab-content').forEach(c=>c.classList.remove('active'));
    const tab = b.dataset.tab; document.getElementById(tab).classList.add('active');
  }));

  const userSearch = document.getElementById('userSearch');
  if(userSearch){ 
    userSearch.addEventListener('input',e=>{
      const q = e.target.value.toLowerCase();
      const activeTab = document.querySelector('.tab-content.active');
      if(activeTab){
        const rows = activeTab.querySelectorAll('tbody tr');
        rows.forEach(tr=>{
          const text = tr.textContent.toLowerCase();
          tr.style.display = text.includes(q)?'table-row':'none';
        });
      }
    }); 
  }

  // Medicine search
  const medicineSearch = document.getElementById('medicineSearch');
  if(medicineSearch){ 
    medicineSearch.addEventListener('input',e=>{
      const q = e.target.value.toLowerCase();
      const rows = document.querySelectorAll('tbody tr');
      rows.forEach(tr=>{
        const text = tr.textContent.toLowerCase();
        tr.style.display = text.includes(q)?'table-row':'none';
      });
    }); 
  }

  // Match results search
  const matchSearch = document.getElementById('matchSearch');
  if(matchSearch){ 
    matchSearch.addEventListener('input',e=>{
      const q = e.target.value.toLowerCase();
      document.querySelectorAll('.match-card').forEach(card=>{
        const visible = card.textContent.toLowerCase().includes(q);
        card.style.display = visible?'block':'none';
      });
    }); 
  }

});