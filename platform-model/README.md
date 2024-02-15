
# Platform Model with ForSyDe IO

## Project Setup (Linux)
```bash
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk install gradle 8.1.1
$ curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | bash && . ~/.jabba/jabba.sh
$ jabba install openjdk@1.17.0

$ gradle build
$ gradle run
```

## Development
- Source for the `.fiodl` creation is located in `app/src/main/java/platform/model/App.java` by programatically connecting the building blocks of the final platform graph.

## Artifacts
Located under `app/src/main/java/platform/artifacts`
- `.fiodl`: ForSyDe IO markup
- `.kgt`: Knowledge Graph visualization format markup (?)
