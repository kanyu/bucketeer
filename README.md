# Bucketeer  &nbsp;[![Build Status](https://travis-ci.com/UCLALibrary/bucketeer.svg?branch=master)](https://travis-ci.com/UCLALibrary/bucketeer) [![Known Vulnerabilities](https://img.shields.io/snyk/vulnerabilities/github/uclalibrary/bucketeer.svg)](https://snyk.io/test/github/uclalibrary/bucketeer)
 
A TIFF to JP2/JPX to S3 bucket microservice. It will turn TIFF images into JPEG 2000 images in two ways:

1) The first way is to convert individual TIFF images into JPEG 2000 images on the local machine. To do this Bucketeer receives individual requests, accesses the TIFFs from a mounted directory, converts TIFFs into JPEG 2000 images, and then uploads them to S3. This method is triggered by a RESTful API.

2) The second way is to convert TIFF images into JPEG 2000 images in batch. With this method, a CSV file is uploaded to Bucketeer and TIFF images from the CSV file, available to Bucketeer from a locally mounted directory, are uploaded to an S3 bucket. An AWS Lambda function picks up on that event and converts the TIFFs into JPEG 2000s. Lastly, the Bucketeer Lambda function stores the JPEG 2000 images in another S3 bucket. This method is triggered by uploading a CSV file through a Web page on the Bucketeer site.

Currently, the CSV upload method is hard-coded for UCLA's particular metadata model. This will be changed to make the process more generic. Examples of UCLA's metadata fields can be found in CSVs in the project's test resources. In the future, there will be actual documentation describing the more generic approach.

## Requirements

* A [Slack](https://slack.com/) Team and a Slack [bot](https://api.slack.com/start/overview), with at least one channel [configured to support file uploads](https://api.slack.com/messaging/files/setup).
    * In order to run tests that use Slack, you should copy the `bucketeer.slack.*` settings out of the sample settings.xml file in `src/test/resources` and copy them into your own settings.xml file (perhaps at `/etc/maven/settings.xml`), supplying the values from your own Slack account.

* An [AWS](https://docs.aws.amazon.com/index.html?nc2=h_ql_doc) account with [S3 buckets](https://docs.aws.amazon.com/s3/?id=docs_gateway) created.
    * In order to run tests that use S3, you should copy the `bucketeer.s3.*` settings out of the sample settings.xml file in `src/test/resources` and copy them into your own settings.xml file (perhaps at `/etc/maven/settings.xml`), supplying the values from your own S3 account.

* A valid license for [Kakadu](https://kakadusoftware.com/)
    * In order to make your copy of Kakadu available to the build, it needs to be placed in its own GitHub repository with the name of the version you've licensed in the root directory (i.e., the root directory will contain a directory named something like `v7_A_7-01642E`). Our current build has only been tested with Kakadu v7. We don't yet support v8. For more details, see the Kakadu section below.

* An installation of [kakadu-lambda-converter](https://github.com/UCLALibrary/kakadu-lambda-converter/) (to use the batch conversion)
    * See that project's GitHub page for information about how to install it.

## Building the Project

The project builds an executable Jar that can be run to start the microservice. To build the project, run:

    mvn package

This will put the executable Jar in the `target/build-artifact` directory.

The application, in its simplest form, can be run with the following command:

    java -Dvertx-config-path=target/test-classes/test-config.properties -jar target/build-artifact/bucketeer-*.jar

To generate the site's Javadocs documentation, run:

    mvn site

This will generate the documentation in the `target/site` directory.

If you'd like to run Bucketeer in a Docker container, you need to have Docker installed and working on your system. To run the version of the build that creates a Docker container and runs tests against that, type:

    mvn verify

_Hint: If you want to run a build without a Docker cache, add -Ddocker.noCache to your mvn command; for instance: `mvn verify -Ddocker.noCache`_

## Running the Bucketeer container

The simplest way to run the newly built Bucketeer container (for development purposes) is to use the Maven Docker plugin. To do that, run:

    mvn docker:start

This will output logging that will tell you what random port Bucketeer has been started on (e.g. http://localhost:32772). If you visit the URL found in the logging output in your browser, you will see the Bucketeer landing page. If you haven't changed the `image.root` location, `src/test/resources/images` will be the source of your images. There are some sample images there used for testing.

If you'd like to change the location where Bucketeer will look for images (to your own test images), you can start the container with a custom `image.root` location:

    mvn docker:start -Dimage.root=/path/to/your/imageroot

To stop the Bucketeer container, when you are done testing, you should run:

    mvn docker:stop

You can always see which containers are running by using Docker's `docker ps` command.

## Running the Application for Development

You can run a development instance of Bucketeer by typing the following within the project root:

    mvn -Plive test

Once run, the service can be verified/accessed at [http://localhost:8888/status](http://localhost:8888/status). The API documentation can be accessed at [http://localhost:8888/docs](http://localhost:8888/docs)

If you want to run the application with a different mount point (for image sources) and file prefix (e.g. the UCLA file path prefix), you can use something like:

    mvn -Plive test -Dbucketeer.fs.mount=/opt/data -Dbucketeer.fs.prefix=UCLAFilePathPrefix

If you leave off the `bucketeer.fs.prefix` Bucketeer will treat the `bucketeer.fs.mount` as the default directory.

## Including Kakadu

To build an image that includes Kakadu, supply two additional build parameters: the repository and the version number; this should look something like:

    mvn verify -Dkakadu.git.repo=scm:git:git@github.com:uclalibrary/kakadu.git -Dkakadu.version=v7_A_7-01642E

Once you've done this, you'll get the following warning:

    warning: adding embedded git repository: src/main/docker/kakadu
    hint: You've added another git repository inside your current repository.
    hint: Clones of the outer repository will not contain the contents of
    hint: the embedded repository and will not know how to obtain it.
    hint: If you meant to add a submodule, use:
    hint: 
    hint:   git submodule add <url> src/main/docker/kakadu
    hint: 
    hint: If you added this path by mistake, you can remove it from the
    hint: index with:
    hint: 
    hint:   git rm --cached src/main/docker/kakadu
    hint: 
    hint: See "git help submodule" for more information.

This is what you want. You do not want to add your Kakadu code as a submodule since the repository is private and should not be linked to this project's code.

UCLA developers only need to supply the correct `kakadu.version` v7 value. The build is set up to use our private Kakadu GitHub repository by default. Non-UCLA developers should not supply `kakadu.version` without also supplying `kakadu.git.repo`, since the UCLA Kakadu repository is a private repository that cannot be accessed by others.

It's important to remember that if you build a Docker container with `kakadu.version`, you must also supply that same argument when you run the `mvn docker:start` and `mvn docker:stop` commands. They will look something like:

    mvn docker:start -Dkakadu.version=v7_A_7-01642E

and

    mvn docker:stop -Dkakadu.version=v7_A_7-01642E

You do not need to supply the `kakadu.git.repo` argument when just starting or stopping your previously built Kakadu-enabled containers. That's only needed at the point of building them.

Kakadu is only needed if you want to do Kakadu in Bucketeer, instead of using Bucketeer to send TIFFs to AWS Lambda to process.

## Tweaking the Batch Upload

Choosing between conversion methods depends largely on how quickly TIFF images can be uploaded to the AWS Lambda bucket. AWS Lambda scales horizontally (up to 1000 simultaneous functions), so if you can upload TIFFs to the S3 bucket faster than they can be processed by the X number of cores on your local machine, it makes sense to use the batch method.

To support getting TIFFs up to S3 as quickly as possible, there are a number of controls in Bucketeer that can be adjusted to improve performance.

<dl>
  <dt>The worker verticle count</dt>
  <dd>The S3 upload verticle is a worker that has its own S3 client. If you configure multiple worker / upload verticles, there will be multiple clients sending TIFF images to the S3 bucket. This is controlled by the <code>s3.uploader.instances</code> property.<br/><br/>When you run the application locally, through the live test method, this can be set in a Maven settings file (for permanent usage) or can be set at runtime (for easier testing) by passing the value in as a system property.<br/><br/><i>Note: If you're using the Docker container mentioned earlier, the value would be passed in as an ENV property or be set in the application's configuration file. See the <a href="https://github.com/uclalibrary/docker-bucketeer">Docker Bucketeer project</a> for more detail.</i></dd>
  <dt>The worker verticle's thread count</dt>
  <dd>Each S3 client (in a worker verticle) can be configured to use one or more threads. Setting the thread count to more than one will allow each S3 client to upload multiple files at a time. The property for this value is <code>s3.uploader.threads</code>. Keep in mind that this doesn't set the total number of threads used, but just the number of threads per S3 client.<br/><br/>This value, like the worker verticle count, can be set in a Maven settings file or passed in via the command line as a system property. In the Docker environment, it should be set through the application's configuration file or as an ENV property.
  <dt>The maximum number of S3 requests</dt>
  <dd>If you set the above two values very high, it's easy to run out of RAM on your machine (since the S3 clients read all the TIFF files concurrently). The maximum number of S3 requests threshold provides an upper limit on the number of S3 PUTs that can be in process at any given time, regardless of the number of worker verticles or threads that have been configured.<br/><br/>The maximum number of requests you allow will depend on the amount of RAM available on the machine. When the maximum has been reached, conversion requests are requeued until there are resources available. The property for this configuration is <code>s3.max.requests</code>.<br/><br/>The ways that it can be set are just like the prior two properties.</dd>
  <dt>The requeuing delay</dt>
  <dd>Bucketeer is built on a messaging platform so, when the maximum number of PUT requests has been reached, any additional requests that come in for conversion will be requeued (until a new upload slot is available). The requeuing delay isn't really a performance configuration, like the above three, but it does allow you to reduce the number of messages flowing through the system by introducing an X number of seconds wait until a message is requeued. The property for this is <code>s3.requeue.delay</code>.<br/><br/>It's fine to leave this with its default value, but if you want to change it you'd do so in the same way as you would for the above properties.</dd>
</dl>

We're still experimenting with different configurations, so we don't have a recommendation for best values, given a particular type of machine, for these properties at this time.

## Contact

We use an internal ticketing system, but we've left the GitHub [issues](https://github.com/UCLALibrary/bucketeer/issues) open in case you'd like to file a ticket or make a suggestion.
