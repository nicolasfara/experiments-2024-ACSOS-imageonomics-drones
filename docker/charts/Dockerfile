FROM python:3.12.4
RUN mkdir /experiment
WORKDIR /experiment
COPY .python-version ./
COPY process.py ./
COPY requirements.txt ./
RUN apt-get update
RUN apt install -y texlive-latex-extra texlive-fonts-recommended dvipng cm-super
RUN pip install --upgrade pip
RUN pip install -r requirements.txt
CMD python process.py
