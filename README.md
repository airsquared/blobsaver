Help support this project by ⭐️'ing it! [Donations](https://www.paypal.me/airsqrd) also appreciated!

# blobsaver [![GitHub All Releases](https://img.shields.io/github/downloads/airsquared/blobsaver/total.svg)](https://github.com/airsquared/blobsaver/releases) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/0d4fdc1daca5402a8c57efc3bef73d31)](https://www.codacy.com/gh/airsquared/blobsaver/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=airsquared/blobsaver&amp;utm_campaign=Badge_Grade)
A GUI for saving SHSH blobs using [tsschecker](https://github.com/tihmstar/tsschecker). Supports macOS, Windows, and Linux.

[Download here](https://github.com/airsquared/blobsaver/releases) (Requires [Java](https://java.com/en/download/manual.jsp))

**Tip:** if you want blobs you save to automatically be uploaded to the cloud, see [here](https://github.com/airsquared/blobsaver/wiki/Automatically-saving-blobs-to-the-cloud).

![Mac Screenshot](.github/screenshots/screenshot-macos.png)
![Windows Screenshot](.github/screenshots/screenshot-windows.png)

## Features
- **Automatically save blobs in the background**
- Store up to ten devices with presets
- Save blobs for beta versions
- Read all the information (including the apnonce) from a connected device, so you don't have to get it manually
- No need to download entire .ipsw for beta versions(just specify link)

## Feedback
Please send feedback via [Github Issue](https://github.com/airsquared/blobsaver/issues/new/choose) or [Reddit PM](https://www.reddit.com/message/compose?to=01110101_00101111&subject=Blobsaver%20Feedback) if you encounter any bugs/problems or have a feature request. 

## Built With
- JDK 8
- [IntelliJ Idea](https://www.jetbrains.com/idea/)
- [Gradle](https://gradle.org/)
- [shadow](https://github.com/johnrengelman/shadow) (gradle plugin)
- [gradle-macappbundle](https://github.com/crotwell/gradle-macappbundle) (Mac) (gradle plugin)
- [gradle-launch4j](https://github.com/TheBoegl/gradle-launch4j) (Windows) (gradle plugin)
- [Inno Setup](http://www.jrsoftware.org/isinfo.php) (Windows) (creating Windows installer)

See the full credits [here](libraries_used.txt).

## License [![GitHub license](https://img.shields.io/github/license/airsquared/blobsaver.svg)](https://github.com/airsquared/blobsaver/blob/master/LICENSE)
This project is licensed under GNU GPL v3.0-only - see the [LICENSE](https://github.com/airsquared/blobsaver/blob/master/LICENSE) file for details
