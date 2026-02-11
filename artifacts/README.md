# Build Artifacts

Attempted to build the Android app with:

```bash
export JAVA_HOME=$HOME/.local/share/mise/installs/java/17.0.2
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
```

The build currently fails before APK generation because the project references Android Gradle Plugin `8.13.2`, which is not resolvable in this environment. See `build.log` for full details.

A downloadable archive of this build attempt is available at:

- `artifacts/build-artifacts.zip`
