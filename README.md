# blobsaver
A GUI for saving SHSH blobs using encounter's fork of tsschecker. Supports both Mac, Windows, and Linux. Requires [Java](https://java.com/inc/BrowserRedirect1.jsp).

If you have an antivirus, select "Always Allow" for anything related to tsschecker or Java. An antivirus may cause blobsaver to crash. If that happens please send feedback.

![Mac Screenshot](https://i.imgur.com/QzhemWh.png)
![Windows Screenshot](https://i.imgur.com/EzJSIVY.png)

## Features
- Store up to ten devices with presets
- Choose where to save blobs with file picker
- Save blobs for beta versions
- No need to download entire .ipsw for beta versions(just specify link)
- Explains how to get ECID, Board Config(if needed), and information necessary for beta versions
- Automatically checks for updates and prompts if available
- Optionally specify device identifier instead of using device picker
- Optionally specify apnonce

## Feedback
Please send feedback via [Github Issue](https://github.com/airsquared/blobsaver/issues/new/choose) or [Reddit PM](https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Feedback) if you encounter any bugs/problems or have a feature request. 

## TODO:
- Use macOS menu bar
- Package into .app/.exe [maybe this](https://github.com/Jorl17/jar2app)
- Automatically save blobs for all signed versions
- Daemon to do it automatically in the background
- Use libimobiledevice to read ECID directly from device (Possibility?)

## Built With
- JDK 8
- [IntelliJ Idea](https://www.jetbrains.com/idea/)
- [Gradle](https://gradle.org/) 

## License
This project is licensed under the GNU GPL v3.0 - see the [LICENSE](https://github.com/airsquared/blobsaver/blob/master/LICENSE) file for details
