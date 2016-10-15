synkr
=====

Simple sync app supporting local file system and AWS S3

Build
=====

syncr uses `sbt` and `sbt-native-packager` which can produce various package types, see the documentation for details: https://github.com/sbt/sbt-native-packager

To build a Debian package:

     sbt debian:packageBin


Configuration
=============

synr is looking for a `synkr.conf` file in `~/.synkr`
which should look like this:

```
aws {
  access.key = "theawskey"
  access.secret = "theawssecret"
}

sync {
  firstsync {
    source = /home/user/folder
    sourceFile = afile.ext
    target = abucket
    targetFile = akeyinthebucket
  }
}
```

License
=======

MIT
