# AmazonQLDB SmokeTest

Simple project to test drive Amazon's QLDB.

## Requirements

### Basic Configuration

You need to set up your AWS security credentials before the sample code is able
to connect to AWS. You can do this by creating a file named "config" at `~/.aws/` 
(`C:\Users\USER_NAME\.aws\` for Windows users) and saving the following lines in the file:

    [default]
    aws_access_key_id = <your access key id>
    aws_secret_access_key = <your secret key>
    region = us-east-1 <or other region>

Alternatively, us the [AWS CLI](https://aws.amazon.com/cli/) and run `aws configure` to 
step through a setup wizard for the config file.

See the [Security Credentials](http://aws.amazon.com/security-credentials) page
for more information on getting your keys.

### Miscellany

* [Java 8 Installation](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)
* [Gradle]()
* [Gradle Wrapper](https://docs.gradle.org/3.3/userguide/gradle_wrapper.html)
* Run `prereqs.sh` to grab the QLDB Java libraries
* If you are using Visual Studio Code, grab the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

## Running

Windows:

```
gradlew run -DsmokeTest=QLDBSmokeTest
```

Unix:

```
./gradlew run -DsmokeTest=QLDBSmokeTest
```
