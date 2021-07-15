This documentation describes how to manually compile the *Docker image*
and upload it to *AWS ECR* (*Amazon Elastic Container Registry*).

In the context of the AIMS eReefs platform, this procedure is automated by a `Jenkins` task. 
This manual process is described here for anyone attempting to create a similar infrastructure.

**NOTE**: For security reason, this document use the fictitious
*AWS Account number* `123456789012`.
You will need to replace it with your own *AWS Account number*.

## Docker
This project is deployed on *AWS ECR* infrastructure.

## Compile
1. `$ cd ~/path/to/project/ereefs-ncanimate2-frame`
2. `$ mvn clean package`
3. `$ cd ~/path/to/project/ereefs-ncanimate2`
2. `$ mvn clean package`
3. `$ docker-scripts/build-docker-image.sh`

## Initialisation
The following steps only needs to be done once.

### Create a AWS Access Key
1. Go to `AWS Services > Security, Identity, & Compliance > IAM`
2. Click on your username (example: `glafond`)
3. Click on the `Security credentials` tab
4. Click `Create access key` button
5. Copy the credentials

### Install AWS Cli on your computer
You will need the `AWS Cli` to authenticate to `AWS`, to push the Docker image to `ECR`.

```
$ sudo apt-get install awscli
$ aws configure
    AWS Access Key ID [XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX]: (enter the key generated earlier)
    AWS Secret Access Key [AKIAXXXXXXXXXXXXXXXX]: (enter the secret generated earlier)
    Default region name [ap-southeast-2]:
    Default output format [json]:
```

Edit the AWS configuration file and move credentials to a *glafond* profile.

```$ vim ~/.aws/credentials```

```
[default]
aws_secret_access_key =
aws_access_key_id =

[glafond]
aws_secret_access_key = XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
aws_access_key_id = AKIAXXXXXXXXXXXXXXXX
```

### Create the Docker repository
Create a repository for your Docker image on `AWS ECR`.
In this documentation, we will be using the `AWS ECR` repository `ereefs-ncanimate-test`
with URL `123456789012.dkr.ecr.ap-southeast-2.amazonaws.com/ereefs-ncanimate-test`.

*AWS* gives a list of directives to follow in order to push your
docker container to the *AWS ECR* repository,
but you should not need them if you follow this guide.
If you still want to see the instructions, select the repository
and click the `View push commands` button.

## Authenticate to AWS using AWS Cli

```$ $(aws ecr get-login --profile glafond --no-include-email --region ap-southeast-2)```

If you are using an older version of *AWS Cli*, you might
get the following error:  
`Unknown options: --no-include-email`

If it's the case, follow this simple workaround:
1. Get the login command by running `$ aws ecr get-login --profile glafond --region ap-southeast-2`
2. Copy / paste the output, remove the `-e none` and execute the command

    Example:

    Output:
    ```
    $ docker login -u AWS -p JcjdAS...cjuQsd -e none https://123456789012.dkr.ecr.ap-southeast-2.amazonaws.com
    ```

    Command to run:
    ```
    $ docker login -u AWS -p JcjdAS...cjuQsd https://123456789012.dkr.ecr.ap-southeast-2.amazonaws.com
    ```

    Expected result:
    ```
    Login Succeeded
    ```

## Upload the Docker image to AWS ECR
```
$ cd ~/path/to/project/ereefs-ncanimate2
$ docker tag ereefs-ncanimate-test:latest 123456789012.dkr.ecr.ap-southeast-2.amazonaws.com/ereefs-ncanimate-test:latest
$ docker push 123456789012.dkr.ecr.ap-southeast-2.amazonaws.com/ereefs-ncanimate-test:latest
```

### Delete old Docker images
The old images occupy spaces on ECR. The storage cost <a href="https://aws.amazon.com/ecr/pricing/">$0.10 per GB-month</a>.
It's good practice to delete them if we are not planning to reuse them.
1. Go to `AWS Services > Compute > ECR`
2. Click `ereefs-ncanimate-test`
3. Select all the `untagged` images (select all using the checkbox on top then unselect the `latest` image)
4. Click the `Delete` button in the top right corner of the page

## Run the Docker image using AWS Batch

### Initialisation

#### Create a Job Role
1. Go to `AWS Services > Security, Identity, & Compliance > IAM > Roles`
2. Click `Create Role`
3. Select type of trusted entity: `AWS service`
    - Choose the service that will use this role: Select `Elastic Container Service`
    - Select your use case: `Elastic Container Service Task`
    - Next: Permissions
4. Attached permissions policies
    - `AmazonS3FullAccess`
    - `AmazonSNSFullAccess`
    - Next: Tags
5. Next: Review
6. Role name: `eReefsNcAnimateRole`
    - Role description: `Role used by the ereefs-ncanimate`
    - Create Role

#### Create a Compute Environment
Create a "Compute Environment" on which the Docker image will run.
This documentation is using the compute environment `ecs_ereefs_100G`.

#### Create a Job Definition
1. Go to `AWS Services > Compute > Batch > Job definitions`
2. Click the `Create` button
3. Create a job definition
    - Job definition name: `ereefs-ncanimate-jobdef`
    - Environment
        - Job role: `eReefsNcAnimateRole`
        - Container image: `123456789012.dkr.ecr.ap-southeast-2.amazonaws.com/ereefs-ncanimate`
        - vCPUs: `1`
        - Memory (MiB): `3072`
    - Create Job Definition

#### Create a Job Queue (if ereefs-large-disk-ondemand doesn't exists)
NOTE: The Queue has changed, I'm not sure the following procedure is still accurate.
1. Go to `AWS Services > Compute > Batch > Job Queues`
2. Click the `Create queue` button
    - Create a job queue
        - Queue name: `ereefs-standard-ondemand`
        - Priority: `1`
    - Connected compute environments for this queue
        - `ecs_ereefs_100G`
    - Create Job Queue
3. Select the new Job Queue

### Run the Job
1. Go to `AWS Services > Compute > Batch > Job definitions`
2. Click on `ereefs-ncanimate-jobdef` to expand it
3. Select the latest revision (example: `Revision 3`) by clicking the radio button
4. `Actions > Submit job`
    - Submit an AWS Batch Job
        - Job run-time
            - Job name: `ereefs-ncanimate-job_2019-02-27_11h56`
            - Job definition: `ereefs-ncanimate-jobdef:3`
            - Job queue: `ereefs-large-disk-ondemand`
            - Job Type: `Single`
    - Submit job

#### Result
1. Go to `AWS Services > Compute > Batch > Jobs`
2. The job appear in Status `submitted`
3. The job status automatically changes to `pending`, `runnable`, `starting`, `running` then `succeeded` OR `failed`
4. Click the task to see the `Job details`, scroll down to `Attempts` and click the `View logs` link
