FROM debian:buster
RUN apt-get update
RUN apt-get install -y maven git sudo
RUN bash -c 'echo -e 'password\npassword\n\n\n\n\n\nY\n' | adduser user'
COPY . /home/user/JavaShebang
RUN mkdir /home/user/bin
RUN chown -R user:user /home/user
RUN su - user -c /home/user/JavaShebang/scripts/install.sh
CMD bash -c 'echo "run: \"sudo -i -u user bash\" to enter user mode"; bash'
