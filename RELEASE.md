# java 1.5+ release

[![daggerok-context](https://www.bintray.com/docs/images/bintray_badge_color.png)](https://bintray.com/daggerok/daggerok/daggerok-context?source=watch)

**build.gradle**

```gradle
repositories {
  mavenCentral()
  // or:
  jcenter()
}

dependencies {
  compile 'com.github.daggerok:daggerok-context:1.0.4'
}
```

[![daggerok-context](http://maven.apache.org/images/maven-logo-black-on-white.png)](https://maven-badges.herokuapp.com/maven-central/com.github.daggerok/daggerok-context)

**pom.xml**

```xml
<dependencies>
  <dependency>
    <groupId>com.github.daggerok</groupId>
    <artifactId>daggerok-context</artifactId>
    <version>1.0.4</version>
  </dependency>
</dependencies>

<!-- only for bintray jcenter repository use cases -->
<repositories>
  <repository>
    <id>jcentral</id>
    <url>https://jcenter.bintray.com</url>
  </repository>
</repositories>
```

## other options

### bintray (user repo) gradle

**build.gradle**

```gradle
repositories {
  maven { url 'https://dl.bintray.com/daggerok/daggerok' }
}

dependencies {
  compile 'com.github.daggerok:daggerok-context:1.0.4'
}
```

### bintray (user repo) maven

**pom.xml**

```xml
<repositories>
  <repository>
    <id>bintray-daggerok</id>
    <url>https://dl.bintray.com/daggerok/daggerok</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.daggerok</groupId>
    <artifactId>daggerok-context</artifactId>
    <version>1.0.4</version>
  </dependency>
</dependencies>
```

### jitpack gradle

**build.gradle**

```gradle
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  compile 'com.github.daggerok:daggerok-context:1.0.4'
}
```

### jitpack maven

**pom.xml**

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.daggerok</groupId>
    <artifactId>daggerok-context</artifactId>
    <version>1.0.4</version>
  </dependency>
</dependencies>
```
