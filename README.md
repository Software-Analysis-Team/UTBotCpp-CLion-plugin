# UTBotCpp-CLion-plugin
<!-- Plugin description -->
Plugin for generating unit tests for C++ files in CLion.

## How to run

### Dummy server

To try plugin with dummy server that does not generate tests, do the following:

1. Run the `main` function in **GrpcServer.kt**. 
2. Then run gradle task `Run Plugin`.

### Original server

#### Setup server using docker 
Right now UTBot can be run directly only on Ubuntu.
For launching on other systems a docker container can be used. 

1. install docker
2. You need to do `docker login` to `ghcr.io` as described [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-to-the-container-registry).
3. `docker pull ghcr.io/unittestbot/utbotcpp/base_env:<image_tag>`, you can choose `image_tag` [here](https://github.com/UnitTestBot/UTBotCpp/pkgs/container/utbotcpp%2Fbase_env).
4. Run the docker container with the following command: 
```
docker run -d \
 --cap-add=SYS_PTRACE --security-opt seccomp=unconfined --security-opt apparmor=unconfined \
 --name=utbot_with_volume_1 \
 -p 2020:2020 \
 -p 2121:2121 \
 --mount type=bind,source=<projects path>,target=/home/utbot/projects \
ghcr.io/unittestbot/utbotcpp/base_env:<image_tag>
```
`projects path` is the path to folder where projects must be located. The is mounted to container and server will be able to access the project files.
5. Then you need build the server. Open CLI in container and do the following in the `home/utbot`: 
```
cp /utbot_distr/install/bin/grpc_cpp_plugin /bin/
git clone --recursive https://github.com/UnitTestBot/UTBotCpp.git
cd UTBotCpp
chmod +x build.sh
./build.sh
```
6. When server is built, you can run it by navigating to `UTBotCpp/server/build` and running
`./utbot server`

#### Setup server on Ubuntu

[How to install and use UTBot](https://github.com/UnitTestBot/UTBotCpp#how-to-install-and-use-utbot)

#### How to run plugin
Launch the plugin by running gradle task `Run Plugin`.
For testing plugin functionality a [c-example](https://github.com/UnitTestBot/UTBotCpp/tree/main/integration-tests/c-example) project is recommended. 
If you used docker to setup server, your project must be located in folder you specified
when running container (`projects path`). You also must specify the remote path in plugin settings: Settings-Tools-UTBot Settings-remote path.
Set it to `/home/utbot/projects/<your project name>`.

Before sending requests for generation, you should do the following: 
1. Add folders containing source files to `source paths` in plugin settings.
2. Launch `Configure Project` by clicking at `UTBot: connected` in status bar.
3. If there are problems, try to delete the build folder and launching `Configure Project` again.
4. When project is configured, you can use actions to send requests to server: by clicking on folders and files in project tree -> UTBot actions,
or by right-clicking in the editor -> UTBot actions -> Generate for function.

<!-- Plugin description end -->