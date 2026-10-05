// Ponte segura entre a tela (renderer) e o processo principal do Electron.
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('launcherApi', {
  launch: (options) => ipcRenderer.invoke('launch', options),
  onLog: (callback) => ipcRenderer.on('launcher:log', (_event, payload) => callback(payload)),
});

contextBridge.exposeInMainWorld('windowApi', {
  minimize: () => ipcRenderer.invoke('window', 'minimize'),
  maximize: () => ipcRenderer.invoke('window', 'maximize'),
  close: () => ipcRenderer.invoke('window', 'close'),
});
