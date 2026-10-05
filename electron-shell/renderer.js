// Logica da tela do launcher.
const $ = (id) => document.getElementById(id);
const log = $('log');

function append(text, type = 'info') {
  const color = type === 'error' ? '#ff8f8f' : '#9aa1b1';
  const time = new Date().toLocaleTimeString('pt-BR');
  log.innerHTML += `\n<span style="color:#5a6072">${time}</span> <span style="color:${color}">${text}</span>`;
  log.scrollTop = log.scrollHeight;
}

window.launcherApi.onLog(({ type, text }) => append(text, type));

$('play').addEventListener('click', async () => {
  const button = $('play');
  button.disabled = true;
  button.textContent = 'Iniciando...';
  append('solicitando login e download do jogo...');

  const result = await window.launcherApi.launch({
    username: $('username').value.trim() || 'Player',
    online: $('online').checked,
    optiFine: $('optifine').checked,
    version: '1.8.9',
  });

  if (!result.ok) {
    append(result.error, 'error');
    button.disabled = false;
    button.textContent = 'Jogar';
  } else {
    button.textContent = 'Jogo iniciado';
  }
});
