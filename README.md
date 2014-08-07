# Android OpenCL Guide

## Introduction
The main purpose of this guide is to have a proper development setup in a Windows environment to program an Android device with an Adreno GPU with OpenCL. However, it is quite easy to setup in a Mac OSX / Linux environment by following the same steps.

In this document, first, the required packages will be listed and the required environment variables. Afterwards, it will be described how to compile and run the template project given in this guide and finally a description of the template project will be given.

## Installation
The template project in this guide was made using Eclipse as IDE and compiled using the Android SDK version 14, the Android NDK r10 and the AdrenoSDK version 3.7.
The links to download the installers for each package are the following:

1. [Eclipse ADT Bundle (Eclipse IDE + Android SDK)](http://dl.google.com/android/ADT-23.0.2.zip)
2. [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html)
3. [Adreno SDK](https://developer.qualcomm.com/mobile-development/maximize-hardware/mobile-gaming-graphics-adreno/tools-and-resources)

While doing the development I found convenient to create a directory where all the required tools are installed as it is easy to keep track of which versions are being used in only one location. For this I created the folder

    C:\Android

Then extracted the contents of the zip file corresponding to the Eclipse ADT Bundle, as well as the contents of the Android NDK and the Adreno SDK. By the end of the installation of all this packages the `C:\Android` folder should contain:
 
    AdrenoSDK
    android-ndk-r10
    eclipse
    sdk
    SDK Manager.exe

Then, it is required to download the Android SDK version 14 by using the `SDK Manager.exe` application. Execute the manager, select all the coresponding packages to the `Android 4.0 (API 14)` and click on `Install # packages`.

![]("./doc/images/AndroidSDK_Install.png")

Once the required SDK version has been installed close the Manager.

## Environment set-up
By experience, I have found that it is a good practice to keep the paths of the SDK's referenced through *environment variables* instead of hardcoding the path within the projects.

###Environment variables
Once the required SDK software has been installed, it is required to setup *environment variables* to avoid any errors during the compilation. 

The required *environment variables* are:

1. ANDROID_SDK
2. ANDROID_NDK
3. ADRENO_SDK

If you have never setup an environment variable in Windows, this is easily done by following the next steps:

1. Press `Win` + `Pause` keys to access the `System Information` panel.
2. Click on `Advanced system settings` on the left.
3. Click on the `Environment Variables` buttons.
4. Under the `System variables` click on the `New...` button.

![]("./doc/images/NewEnvVarWin.png")

If you have followed the guide step by step up to this point, set the *environment variable* with the following values:

| Name | Value          |
| ------------- | ----------- |
| ANDROID_SDK      | C:\Android\sdk |
| ANDROID_NDK     | C:\Android\android-ndk-r10     |
| ADRENO_SDK     | C:\Android\AdrenoSDK\Development     |

Otherwise, just set the variables values with the corresponding path of your installed packages.

After these variables are all set, add to the `Path` variable:

    %ANDROID_SDK%\tools; %ANDROID_SDK%\platform-tools; %ANDROID_NDK

###Eclipse setup
Once the *env. vars* have been setup launch the Eclipse IDE that came with the bundle found on `C:\Android\eclipse\`.
Set the workspace directory to the path where you have downloaded this code as in `C:\Anroid\Devel\`.

#### ADT plug-in
First of all we need to install the `Android Developer Tools` plugin to Eclipse. For this, in the toolbar select:

    Help > Install new software... >
    
Click on the `Add` button in the Install window and set the following repository:

Name
:   Android Developer Tools Update Site

Location
:   https://dl.google.com/android/eclipse/

![]("./doc/images/ADT_Repository.png")

Select under `Development Tools` the `Android Native Development Tools` click on the `Next` button, accept the agreement and install the plug-in.

#### NDK setup
Before we start coding we need to tell Eclipse where the NDK is located. For this, on the tool bar select:

    Window > Preferences > Android > NDK

and set the `NDK Location` to `C:\Android\android-ndk-r10` or the path where you have installed the Android NDK.

## Project Import
Up to this point you should have the proper environment to compile and execute the AndroidCL application provided in this guide. However we still need to import the project into Eclipse. 

To import the project, once again in the tool bar click on:

    File > Import > General > Existing Projects into Workspace

Then click on the `Next` button. On the window that pops, click on the `Browse` button, select the directory where you downloaded this project, click on the checkbox and finally on the `Finish` button.

![]("./doc/images/ImportProject.png")

The project now will appear on the left hand side of the IDE as `androidcl`. `Right-click` on it and select:

    Android Tools > Add Native Support

### Importing libOpenCL.so
Finally in order to be able to compile and execute the project on the device you need to import the OpenCL compiled library that comes with your device, usually called `libOpenCL.so`. 

The library is located on:

    /system/vendor/lib
    
We need to copy that static library to our project. For this we will use the ADB tool from the command line. First we need to connect the device via the USB cable. To chech the device is properly connected call the following command from the CLI:

    adb devices
    
And a list of the Android connected devices will appear. If this does not happen, make sure you have properly setup the device in USB debugging mode and that you have properly setup the `Path` environment variable with the Android SDK tools path.

With the mobile device properly connected, algo within the CLI go to `$(PROJECT_PATH)\jni\libs` path and input the following command:

    adb pull /system/vendor/lib/libOpenCL.so
    
The library should be copied to the path from where the command was called.

Now you can try to `Run` the project on the device and there should be no problem at all.

## Project description
In this section a brief description of the project will be given to let you have a general overview of the main purpose of the application and how it works.

### General overview

The main task of this application up to this point is to create a Laplacian filter on real time to the video feed incoming from the camera. In the figure below it is depicted how the application works, on how the classes interact.

![]("./doc/images/Project_Overview.png")

First the application runs mainly in the LiveFeatureActivity. In this class all the GUI elements are loaded as well as the OpenCL `kernels` are compiled. 

While programming OpenCL it is important to remember that the programs run on the GPU are compiled on run time and loaded into the GPU.


### Events
The display output of our processed video feed will be on a `SurfaceView` class which requires a `FrameLayout` GUI element to draw on.

The `CameraPreview` as you can tell extends a `SurfaceView`. This class is of our interest as it has the proper callbacks for drawing and we would also like to implement in the `CameraPreview` class as a `Camera.PreviewCallback` to handle the incoming images from the camera.

The `Camera` object, which is the interface class that the Android SDK gives us o handle the camera, is created on the `surfaceChanged` event within our `CameraPreview` class. First we properly initialize the camera by telling it the desired size of the output images, the image format and the FPS desired from the video feed. In this event, the orientation of the camera to be in horizontal and the handles for the callback events are also set. The callbacks are set to be the same `CameraPreview` class.

During the `surfaceChanged` event we stop the camera from sending us images and update our painting buffer. In this application we decided to use the double buffer painting technique to avoid flickering on the painted surface.

The `mVideoSource` buffer is the one that receives the data from the camera and processed, while the `mBackBuffer` is the one that painted into the display surface.

In the `onPreviewFrame` event incoming from the `Camera` we process the incoming image. This byte buffer is fed into the JNI processing class which can be found in the `processor.cpp` file under the `jni` folder.

The `runfilter` method in our native code is fed with the camera output buffer. The pointer to that memory is passed to the `helper` method and loaded into the GPU's memory with:

    cl::Buffer bufferIn = cl::Buffer(gContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                isize*sizeof(cl_uchar), in, NULL);

### OpenCL Kernels
So far the image is stored in the `bufferIn` buffer. This in turn will be passed to the kernels which were previously compiled on the intializatin of the `LiveFeatureActivity`. When the GPU kernels were compiled this were stored in the global memory as `gNV21Kernel`, `gLaplacianK` and `gGaussianK`.

The kernel methods are declared in the `kernels.cl` file which can be located under the directory `assets`. It is important to remember, that this code is compiled in run-time, which in turn is loaded in the GPU to be used later on.

## Future Work
Up to this point we have achieved a template project to develop on Android with OpenCL exploiting the GPU device. However, there is still work ahead to be completed, such as:

1. Implement the Pyramid of Gaussians from the incoming images.
2. Implement a scaling algorithm in the OpenCL kernels.
3. Implement the Pyramid of Laplacians.
4. Calculate the Points of Interest according to Lowe.
5. Implement the Optical Flow and/or Dense Trajectories from two different frames.