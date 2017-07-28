Multimedia Geotagging (Demo Version)
======
Contains a demo version of <a href="https://github.com/socialsensor/multimedia-geotagging">multimedia-geotagging</a> that is the implementation of algorithms that estimate the geographic location of multimedia items based on their textual content and metadata. Also, <a href="https://github.com/socialsensor/geo-util">geo-util</a> is used in order to get the city and country name of the estimated location.


<h2>Instructions</h2>

The containing jar file is the implementation of the algorithm. It takes as input a file whose lines are different sentences. Every sentence is used as a query for the language model that have been built, in order to calculate the Most Likely Cell (MLC) based on pre-calculated word-cell probabilities. This MLC is the final location estimation for every sentence. The output is a file that contains all the estimated location (MLCs) and the countries that they belong for all sentences. Each line of the output file corresponds to the respective line of the input file.

An example is provided in <a href="https://github.com/MKLab-ITI/multimedia-geotagging/blob/demo/src/test/java/gr/iti/mklab/test/MultimediaGeolocator.java">MultimediaGeolocator.java</a>. In order to run it the follow arguments have to be given.<br>
`args[0]` : the root directory of the project<br>
`args[1]` : input file that contains the query sentences<br>
`args[2]` : the pathname of the output file

_Requirements_<br>
To download the required files, you may run `requirements.sh`. Otherwise, they have to be manually downloaded.
The root directory must contain a folder named <a href="https://www.dropbox.com/sh/6v7fz50saldiq9g/AABfyc9Zxe1kE4k3Sf-xNJyDa?dl=0">multi-geo-utils</a> (<a href="https://www.dropbox.com/s/7vdyarczp97r60c/multi-geo-utils.zip?dl=0">zip</a>) that contains the files:
* <a href="http://download.geonames.org/export/dump/countryInfo.txt">`countryInfo.txt`</a>
* <a href="http://download.geonames.org/export/dump/cities1000.zip">`cities1000.txt`</a>
* <a href="https://www.dropbox.com/s/cxfn1h5toxbljzv/term_cell_probs.txt?dl=0">`term_cell_probs.txt`</a>

_Input File_<br>
Each individual line of the input line have to be a different query sentence, otherwise they are going to be considered as one and the estimation will be for both sentences. 

_Output File_<br>
The output file is going to be stored in the specified path with the proper name, based on the given arguments. Each line corresponds to the respective line of the input file. The format of the result line is the following:

`estimated location`<br>
`city, country`<br>
`confidence`<br>
`associated words with the Most Likely Cell`

The different result features are separated by tab character. For the sentences that an estimation can not be made the respective lines have the record `N/A`.

In order to run the jar file, there must be in the same directory the folder <a href="https://www.dropbox.com/sh/6v7fz50saldiq9g/AABfyc9Zxe1kE4k3Sf-xNJyDa?dl=0">util</a> and the file <a href="https://github.com/socialsensor/multimedia-geotagging/blob/demo/log4j.properties">log4j.properties</a>.

A short test file and its results may be found <a href="https://github.com/socialsensor/multimedia-geotagging/blob/demo/test.txt">here</a> and <a href="https://github.com/socialsensor/multimedia-geotagging/blob/demo/test_out.txt">here</a>, respectively.



<h3>Contacts for further details about the project</h3>

Giorgos Kordopatis-Zilos (georgekordopatis@iti.gr)<br>
Symeon Papadopoulos (papadop@iti.gr)
