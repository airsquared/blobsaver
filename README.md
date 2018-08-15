# blobsaver
A GUI for saving SHSH blobs using encounter's fork of tsschecker(uses tihmstar's original for linux). Supports both Mac, Windows, and Linux. Requires [Java](https://java.com/inc/BrowserRedirect1.jsp).

[Download here](https://github.com/airsquared/blobsaver/releases/latest)

If you have an antivirus or firewall, you may need to disable some other settings or disable the firewall completely for automatically saving blobs in the background to work. If you use Norton, go to Settings -> Firewall -> Advanced Program Control and set the option "Low Risk Applications" to "Allow".

![Mac Screenshot](https://i.imgur.com/czq78Yf.png)
![Windows Screenshot](https://i.imgur.com/zlPh4JY.png)

## Features
- **Automatically save blobs in the background**
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
- Auto-upload to Dropbox/Google Drive
- Package into .app/.exe [maybe this](https://github.com/Jorl17/jar2app)
- Better notifications

## Built With
- JDK 8
- [IntelliJ Idea](https://www.jetbrains.com/idea/)
- [Gradle](https://gradle.org/) 

## License
This project is licensed under GNU GPL v3.0-only - see the [LICENSE](https://github.com/airsquared/blobsaver/blob/master/LICENSE) file for details
