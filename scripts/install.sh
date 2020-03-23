#!/bin/bash

cd "$(dirname `readlink -f "$0"`)"
cd ..

mvn package
mkdir -p ~/bin
cat <<EOF > ~/bin/jsb
#!/bin/bash
java -jar $(readlink -f ./target/jsb-jar-with-dependencies.jar) "\$@"
EOF
chmod 755 ~/bin/jsb
