# Scala.js Electron Project Template

A somewhat advanced template for a Scala.js project targeting Web, MacOS, Windows and Linux. 

Based on [Scala.js Project Template](https://github.com/Aurora12/scalajs-project-template), see details there. 

The project shows common stuff one implements in a real-world crossplatform app for Web and Desktop.

* The project produces 
  * Web files, 
  * Exe setup file for Windows,
  * Zip and Dmg files for MacOS,
  * AppImage file for Linux,
  * YML files for auto-updating.
* Uses [Electron Builder](https://github.com/electron-userland/electron-builder).
* Uses auto-update mechanism (see `/resources/templates/electron_main.js`).
* Uses event-based communication between main desktop process and web app inside.
* Uses application system badge (normally used to show update count).
* Shows accessing Electron-specific services from web app.
* Prevents desktop app from closing until web app reports it is ready to close. 

## Usage

Build target folder is `bin` in project root. That's where all of the resources are copied and where the `fastOptJs`/`fullOptJS` output is directed.

The `version` build parameter is optional, it defaults to the `version` setting in `build.sbt`.

### Clean and build from command line

#### Web

`/build-web-dev.sh -Dversion="0.1.0"` 

or
 
`/build-web-release.sh -Dversion="0.1.0"`

Open `/bin/web/index.html` to see the result.

#### Electron

`/build-electron-dev.sh -Dversion="0.1.0"` 

or
 
`/build-electron-release.sh -Dversion="0.1.0"`

Open `/bin/electron/dist/` to see the results for all platforms.

### Clean and build from SBT's interactive mode

Start SBT with `sbt -mem 2000 -Dversion="0.1.0"`. The memory setting here prevents SBT from crashing with out of memory exception after a dozen rebuilds in interactive mode.

#### Web:
* Use `release` or `dev` commands to *clean*, *build* and *collect resources*. 
* Use `webRelease` or `webDev` to *build* and *collect resources*.
* Use `fastOptJS` or `fullOptJS` as you normally would with any Scala.js project to just *build main javascript files*.

#### Electron:
* Use `electronMac`, `electronWin`, `electronLinux` to *clean* & *build* release version of Web and Electron app for a platform.
* Use `electron mac`, `electron win`, `electron linux` to build Electron app using current web build. This way you can use fast-optimized JavaScript in Electron build. This will fail if you don't build for web beforehand.    

## Project structure

* `/bin` – this is where all build files go. The directory is created by build commands and is deleted completely by `clean` command.
* `/bin/web` – Web build.
* `/bin/electron/dist` – Electron build.
* `/project` – a standard SBT project directory, which contains `BuildUtils.scala`, that does file manipulation during build and launches Electron build.
* `/resources` – contains static resources (like css and images) and templates (html, js, json) that get processed by build logic.
* `/src/main/resources/` – the standard directory for js libraries. See `js/example.js` there.

