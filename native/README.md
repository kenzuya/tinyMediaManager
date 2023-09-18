# Native libs/binaries

## libmediainfo

* Windows - Windows 7 - 11, 64bit, https://mediaarea.net/en/MediaInfo/Download/Windows
* Linux - Ubuntu 20.04 (for best compatibility), 64 bit, https://mediaarea.net/en/MediaInfo/Download/Ubuntu
* Linux - RAspbian 10 (for best compatibility), arm 64 bit, https://mediaarea.net/de/MediaInfo/Download/Raspbian
* macOS - macOS 10.10 - 12, https://mediaarea.net/en/MediaInfo/Download/Mac_OS

## libtinyfiledialogs

* Windows - Windows x64 - https://sourceforge.net/projects/tinyfiledialogs/files/
* Linux - self compiled - https://sourceforge.net/projects/tinyfiledialogs/files/

Linux x64:

```
docker run --rm -v "$PWD":/usr/src/myapp -w /usr/src/myapp gcc:9-buster bash -c 'gcc  -ansi -std=gnu89 -pedantic -Wstrict-prototypes -Wall -fPIC -c tinyfiledialogs.c && gcc tinyfiledialogs.o -shared -o libtinyfiledialogs.so'
```

Linux arm32:

```
docker run --rm -v "$PWD":/work dockcross/linux-armv7-lts bash -c '$CC  -ansi -std=gnu89 -pedantic -Wstrict-prototypes -Wall -fPIC -c tinyfiledialogs.c && $CC tinyfiledialogs.o -shared -o libtinyfiledialogs.so'
```

## FFmpeg

* Windows - 64 bit, https://www.gyan.dev/ffmpeg/builds/
* Linux - 64 bit & armhf, https://johnvansickle.com/ffmpeg/
* macOS - https://evermeet.cx/ffmpeg/

