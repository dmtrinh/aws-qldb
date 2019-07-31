#!/usr/bin/env sh

echo

##################################
# Grab required packages
##################################
echo 'Downloading required packages...'
echo '========================================'
# AWS SDK for Java
echo 'AWS SDK for Java'
curl -O https://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip

# QLDB Java Client
echo 'QLDB Java Client'
cd lib && { curl -O https://s3.amazonaws.com/amazon-qldb-assets/sdk/AWSQLDBJavaClient-1.11.x.jar ; cd -; }

# QLDB Driver for Java
echo 'QLDB Driver for Java'
cd lib && { curl -O https://amazon-qldb-assets.s3.amazonaws.com/drivers/java/amazon-qldb-driver-0.1.0-beta.jar ; cd -; }

# Additional Prerequisites
echo
echo '========================================'
echo 'You will also want to create a dedicated IAM User with QLDB access.'
echo 'Grab the access keys for this new IAM User.'
echo 'Set up your AWS credentials and region for revelopment.  Refer to'
echo '  https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html'
echo
