cacoo diagram contents api sample
=================================

This application is a sample to understand how to use [the diagram contents API](http://cacoo.com/lang/en/api_diagram_contents).  
This sample uses Java Swing Application and includes how to get Java and HTML source outputs and how to set up EC2.

このアプリケーションは[図の内容取得API](http://cacoo.com/lang/ja/api_diagram_contents)の使い方を理解するためのサンプルです。  
これはJavaのSwingアプリケーションです。  
Javaソース出力、HTMLソース出力、EC2のセットアップのサンプルが含まれています。

how to operate
--------------

![how to operate(en)](https://cacoo.com/diagrams/82DO48lVNFn9uXYc-ABC5B.png)

![how to operate(ja)](https://cacoo.com/diagrams/82DO48lVNFn9uXYc-5E77F.png)


sample diagrams
---------------

- [Contents API Sample (Output Java Source)](https://cacoo.com/diagrams/fiNXi7g3cquoaLMu)
- [Contents API Sample (Output HTML Source)](https://cacoo.com/diagrams/uYLmRZomNIc0ngG5)
- [Contents API Sample (Setup EC2)](https://cacoo.com/diagrams/p34VEcoIoROfDM4k)

environment
-----------

- Java 6 or newer
- Maven 2.2 or newer

mvn command
-----------

create eclipse project

    $ mvn eclipse:eclipse

compile and create jar file

    $ mvn package

run this sample

    $ mvn exec:java