# AWS Deployment

The deployment of NcAnimate is intended to be automated using
[Jenkins](https://www.jenkins.io/), an automation server.

## Jenkins setup

1. Log yo your Jenkins server
2. Create a new *Job*
    - General
        - Display name: `ereefs-ncanimate2`
    - Branch Sources
        - Git
            - Project Repository: `git@github.com:open-aims/ereefs-ncanimate2.git`
            - Property strategy: `All branches get the same properties`
    - Build Configuration
        - Mode: `by jenkinsfile`
        - Script Path: `Jenkinsfile`
    - Click the *[Save]* button
3. Click on the *ereefs-ncanimate2* job
4. Click the *Scan Multibranch Pipeline Now* link in the left menu
    to load the *main* and the *production* branches.

## Test environment

1. Log yo your Jenkins server and click on the *ereefs-ncanimate2 >> main* job
2. Click on *Build with Parameters*
3. Choose: *deployTarget: [Testing]*
4. Choose: *executionEnvironment: [test]*
5. Click *[Build]*

## Production environment

1. Log yo your Jenkins server and click on the *ereefs-ncanimate2 >> production* job
2. Click on *Build with Parameters*
3. Choose: *deployTarget: [Production]*
4. Choose: *executionEnvironment: [prod]*
5. Click *[Build]*

## After deployment

The deployment process creates a new Docker container in AWS ECR. Storage on AWS is not free.
If you do not plan to reuse the old image, it would be wise to delete it.

1. Log on AWS: https://console.aws.amazon.com/console/home
2. Navigate to *Amazon Container Services > Amazon ECR > Repositories*
3. Select *ereefs-ncanimate-test* or *ereefs-ncanimate-prod*
4. Select all images but the latest and click *[Delete]*
