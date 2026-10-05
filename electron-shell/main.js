// ---------------------------------------------------------------------------
// Casca em Electron do Solar Client Launcher.
//
// O Electron nao reimplementa nada: ele cria a janela, mostra o progresso e
// roda o launcher Java (que faz login, download e sobe o Minecraft). A
// vantagem e a interface moderna sem exigir JavaFX/Swing no launcher.
//
// Uso:
//   cd electron-shell
//   npm install
//   npm start -- --user NomeDoJogador
// ---------------------------------------------------------------------------
const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const { spawn } = require('child_process');
const path = require('path');

const MIN_WIDTH = 520;
const MIN_HEIGHT = 380;

let mainWindow = null;
let gameProcess = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 960,
    height: 620,
    minWidth: MIN_WIDTH,
    minHeight: MIN_HEIGHT,
    frame: false,
    backgroundColor: '#0b0c12',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// ---------------------------------------------------------------------------
// IPC: a tela chama o launcher Java
// ---------------------------------------------------------------------------
ipcMain.handle('launch', async (_event, { username, online, optiFine, version }) => {
  if (gameProcess) {
    return { ok: false, error: 'O jogo ja esta aberto.' };
  }

  const launcherJar = path.join(__dirname, '..', 'launcher', 'build', 'libs',
    'solar-client-launcher-0.1.0.jar');
  const gsonJar = findGson();

  if (!require('fs').existsSync(launcherJar)) {
    return { ok: false, error: 'Launcher nao encontrado. Rode: gradle :launcher:jar' };
  }

  const args = [
    '-cp',
    [launcherJar, gsonJar].filter(Boolean).join(path.delimiter),
    'com.solarclient.launcher.Launcher',
    '--user', username || 'Player',
    '--version', version || '1.8.9',
  ];
  if (online) args.push('--online');
  if (!optiFine) args.push('--no-optifine');

  const java = process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, 'bin', 'java')
    : 'java';

  gameProcess = spawn(java, args, { cwd: path.join(__dirname, '..') });

  const send = (type, text) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('launcher:log', { type, text });
    }
  };

  gameProcess.stdout.on('data', (data) => send('info', data.toString().trimEnd()));
  gameProcess.stderr.on('data', (data) => send('error', data.toString().trimEnd()));
  gameProcess.on('close', (code) => {
    send('info', `Launcher encerrou com codigo ${code}.`);
    gameProcess = null;
  });

  return { ok: true };
});

ipcMain.handle('window', (_event, action) => {
  if (!mainWindow) return;
  if (action === 'minimize') mainWindow.minimize();
  if (action === 'close') mainWindow.close();
  if (action === 'maximize') {
    mainWindow.isMaximized() ? mainWindow.unmaximize() : mainWindow.maximize();
  }
});

function findGson() {
  // procura o gson no cache do gradle (o launcher precisa dele em runtime)
  const gradleCache = path.join(process.env.HOME || '', '.gradle', 'caches');
  try {
    const { execSync } = require('child_process');
    return execSync(
      `find "${gradleCache}" -name "gson-2.8.9.jar" 2>/dev/null | head -1`
    ).toString().trim();
  } catch {
    return '';
  }
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (gameProcess) gameProcess.kill();
  if (process.platform !== 'darwin') app.quit();
});
