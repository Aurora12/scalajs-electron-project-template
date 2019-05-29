'use strict'

const appId = "%appId%";
const electron = require('electron');
const {autoUpdater} = require('electron-updater');
const Config = require('electron-store');
const path = require('path');
const url = require('url');
const log = require("electron-log");

require('electron-context-menu')({
	prepend: (params, browserWindow) => [{
		label: 'Copy image to buffer',
		// Only show it when right-clicking images
		visible: params.mediaType === 'image',
		click(item, win) {
      win.webContents.copyImageAt(params.x, params.y)
    }
	}]
});

const {app, BrowserWindow, ipcMain, clipboard, nativeImage, dialog} = electron
const config = new Config()

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow;

function createWindow() {
  const { width, height } = electron.screen.getPrimaryDisplay().workAreaSize
  let opts = {
    title: '%productName%',
    icon: path.join(__dirname, 'img/icon.png'),
    webSecurity: true,
    width: width,
    height: height
  }

  mainWindow = new BrowserWindow(opts)

  mainWindow.loadURL(url.format({
    pathname: path.join(__dirname, 'index.html'),
    protocol: 'file:',
    slashes: true
  }))

  mainWindow.on('close', function () {
    config.set('winBounds', mainWindow.getBounds())
  })

  mainWindow.on('closed', function () {
    // Dereference the window object, usually you would store windows
    // in an array if your app supports multi windows, this is the time
    // when you should delete the corresponding element.
    mainWindow = null
  })

  //open links externally by default
  mainWindow.webContents.on('new-window', function(event, url) {
    event.preventDefault()
    electron.shell.openExternal(url)
  })

  if (process.platform == "darwin" || process.platform == "win32") {
    let template = [
      {
        label: app.getName(),
        submenu: []
      },
      {
        label: 'Edit',
        submenu: [
          {role: 'undo'},
          {role: 'redo'},
          {type: 'separator'},
          {role: 'cut'},
          {accelerator: "CmdOrCtrl+C", label: "Copy", selector: "copy:"},
          {accelerator: "CmdOrCtrl+V", label: "Paste", selector: "paste:"},
          {role: 'selectall'}
        ]
      },
      {
        label: 'View',
        submenu: [
          {role: 'toggledevtools'},
          {type: 'separator'},
          {role: 'resetzoom'},
          {role: 'zoomin'},
          {role: 'zoomout'},
          {type: 'separator'},
          {role: 'togglefullscreen'}
        ]
      },
      {
        role: 'window',
        submenu: [
          {role: 'minimize'},
          {role: 'zoom'},
          {role: 'front'},
          {type: 'separator'},
          {role: 'close'}
        ]
      }
    ]

    if (process.platform == "darwin") {
      template[0].submenu = [
        {role: 'about'},
        {type: 'separator'},
        {role: 'hide'},
        {role: 'hideothers'},
        {role: 'unhide'},
        {type: 'separator'},
        {role: 'quit'}
      ]
      template[1].submenu.push(
        {type: 'separator'},
        {
          label: 'Speech',
          submenu: [
            {role: 'startspeaking'},
            {role: 'stopspeaking'}
          ]
        }
      );
    } else {
      template[0].submenu = [
        {role: 'about'},
        {role: 'quit'}
      ]
    }

    const menu = electron.Menu.buildFromTemplate(template)
    electron.Menu.setApplicationMenu(menu)
  }
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', function () {
  createWindow()
  webConsole.warn("Process arguments: ["+process.argv.join(",")+"]")
})

// Quit when all windows are closed.
app.on('window-all-closed', function () {
  app.quit()
})

app.on('activate', function () {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (mainWindow === null) {
    createWindow()
  }
})

app.on('web-contents-created', function (event, wc) {
  wc.on('before-input-event', function (event, input) {
    if ((input.key == "r" && input.control) || input.key == "F5") {
      event.preventDefault()
    }
  })
})

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.

let webConsole = function(){
  var o = {
    info: function(message) {
      console.info(message.toString())
      mainWindow.webContents.send("info", message.toString())
    },
    warn: function(message) {
      console.warn(message.toString())
      mainWindow.webContents.send("warn", message.toString())
    },
    error: function(message) {
      console.error(message.toString())
      mainWindow.webContents.send("error", message.toString())
    }
  }
  o.log = o.info
  return o
}();

function notify(title, message) {
  mainWindow.webContents.send("notify", title.toString(), message.toString())
}

function onUpdateBadgeIcon(e, dataUrl) {
  if (dataUrl != null && dataUrl.toString().length > 0) {
    var img = nativeImage.createFromDataURL(dataUrl)
    mainWindow.setOverlayIcon(img, 'You have unread messages!')
  } else {
    mainWindow.setOverlayIcon(null, '')
  }
}

if (process.platform == "win32") {
  ipcMain.on('updateBadgeIcon', onUpdateBadgeIcon)
}

autoUpdater.logger = log
autoUpdater.logger.transports.file.level = "info"
autoUpdater.autoDownload = false
autoUpdater.allowDowngrade = false

function checkForUpdates() {
  webConsole.log("Sending update check request")
  autoUpdater.checkForUpdates().then(
    (updateCheckResult) => {
      webConsole.warn("Update check success: "+updateCheckResult.updateInfo.version+" vs. %version%")
      if (updateCheckResult.updateInfo.version != "%version%") {
        if (!autoUpdater.allowDowngrade) {
          var current = "%version%".split(".");
          var next = updateCheckResult.updateInfo.version.split(".");
          for (var i = 0; i<current.length; i++) {
            var c = parseInt(current[i]);
            var n = parseInt(next[i]);
            if (c != n) {
                if (c > n) {
                    webConsole.warn("Current version ["+current+"] is bigger than the ["+next+"] on the server!")
                    return;
                } else {
                    webConsole.warn("Current version ["+current+"] is lower than the ["+next+"] on the server!")
                    break;
                }
            }
          }
        }
        mainWindow.webContents.send("requestUpdate", updateCheckResult.updateInfo.version)
      }
    },
    (reason) => { 
      webConsole.error("Update check error: "+reason) 
    }
  )
}

autoUpdater.signals.updateDownloaded((info) => {
  webConsole.warn(
    "update downloaded signal: "+info.version+", "+info.releaseDate
  )
  if (process.platform == "win32") {
    autoUpdater.quitAndInstall(true, true)
  } else {
    autoUpdater.quitAndInstall()
  }
})
autoUpdater.signals.progress((progressInfo) => {
  var percent = Math.round(progressInfo.percent).toString()
  mainWindow.webContents.send("updateProgress", percent)
  webConsole.warn(
    "update progress signal: "+progressInfo.transferred+"/"+progressInfo.total+" ("+progressInfo.percent+")"
  )
})
autoUpdater.signals.updateCancelled((info) => {
  webConsole.warn("update cancelled signal: "+info)
})
autoUpdater.on("error", (event, info) => {
  webConsole.error("update error: "+info)
})
ipcMain.on('acceptUpdate', () => {
  webConsole.warn("Update accepted, starting download...")
  autoUpdater.downloadUpdate()
})
var updateTimerId
ipcMain.on('checkForUpdate', () => {
  webConsole.log("Start checking for updates.")
  clearTimeout(updateTimerId)
  clearInterval(updateTimerId)
  updateTimerId = setTimeout(function(){
    checkForUpdates()
    updateTimerId = setInterval(checkForUpdates, 1000*60*30)
  }, 1000*5)
})

ipcMain.on('askToQuit', (event, text, labelYes, labelNo) => {
  dialog.showMessageBox(mainWindow, {
      type: "question",
      buttons: [labelYes, labelNo],
      defaultId: 0,
      message: text,
      cancelId: 1
    },
    (result) => {
      webConsole.warn("Dialog result: "+result)
      if (result == 0) {
         mainWindow.webContents.send("confirmQuit")
      }
    }
  )
});
ipcMain.on('readyToQuit', () => {
  webConsole.warn("App is ready to quit!")
  app.quit()
});