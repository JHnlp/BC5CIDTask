[Directory]

A. About
B. Prerequisites
C. Prerequisites
D. Instructions for running DNorm
J. References


#======================================================================================#

A. [About]

	This chemical-induced disease relation extraction tool is implemented for the BioCreative-V CID subtask. To see the details about the task, please visit http://www.biocreative.org/tasks/biocreative-v/track-3-cdr/.


B. [Prerequisites]

	Before running this relation extraction tool, please be sure of that you have installed Java8.

	Make sure that you have got enough permissions to run the script "bllip-parse.sh" before running this tool.


C. [Instructions for running Demo]

	You can using the following command lines in the shell to run the demo:
		java -Xmx10G -Xms10G -cp ".:%JAVA_HOME%/lib:%JAVA_HOME%/lib/dt.jar:%JAVA_HOME%/lib/tools.jar:./bin/:./lib/*" gjh.bc5.main.DemoRunner -D1
	or
		java -Xmx10G -Xms10G -cp ".:%JAVA_HOME%/lib:%JAVA_HOME%/lib/dt.jar:%JAVA_HOME%/lib/tools.jar:./bin/:./lib/*" gjh.bc5.main.DemoRunner -D2
		
	The parameter "-D1" means to use the model trained only by the training set to predict on the development set using gold entity annotaion.

	Results for "-D1":
		Precision: 0.6192946058091287
		Recall: 0.5899209486166008
		F-score: 0.6042510121457491

	The parameter "-D2" means to use the model trained by the training+development set to predict on the test set using gold entity annotaion.

	Results for "-D2":
		Precision: 0.6198521647307286
		Recall: 0.550656660412758
		F-score: 0.5832091405861898

	The files used for the demos are located in the directory "demo/examples/dataset/".


D. [Instructions for running this tool for new files]

	Tab-delimited txt files in the Pubtator format are used as the input for relation extraction. These files should be in the directory of "input", and the corresponding output files would be showed in the directory of "output" after the processing.

	In the Pubtator format, the first row is title, and second row is abstract. The rows below abstract are bio-concept mentions. Between any two articles, a blank line is required. 
	There are six attributes used for describing the entity annotation in Pubtator format, separated by Tab keys. The six attributes are: 

		PMID<tab>START OFFSET<tab>END OFFSET<tab>text MENTION<tab>mention TYPE (e.g. Disease)<tab>database IDENTIFIER<tab>Individual mentions

	Note that the last attribute "Individual mentions" is optional. It is only annotated once the MENTION is a composite mention. 
	The START OFFSET is the first character offset of the mention while END OFFSET is the last. 

	Example:
		2083961|t|Acute renal failure due to rifampicin.
		2083961|a|A 23-year-old male patient with bacteriologically proven pulmonary tuberculosis was treated with the various regimens of antituberculosis drugs for nearly 15 months. Rifampicin was administered thrice as one of the 3-4 drug regimen and each time he developed untoward side effects like nausea, vomiting and fever with chills and rigors. The last such episode was of acute renal failure at which stage the patient was seen by the authors of this report. The patient, however, made a full recovery.
		2083961	0	19	Acute renal failure	Disease	D058186
		2083961	27	37	rifampicin	Chemical	D012293
		2083961	96	118	pulmonary tuberculosis	Disease	D014397
		2083961	205	215	Rifampicin	Chemical	D012293
		2083961	325	331	nausea	Disease	D009325
		2083961	333	341	vomiting	Disease	D014839
		2083961	346	351	fever	Disease	D005334

	The file named "sample.txt" in the root directory is an example Pubtator file.

	To run this tool for new Pubtator files, you may use the following command line:

		java -Xmx10G -Xms10G -cp ".:%JAVA_HOME%/lib:%JAVA_HOME%/lib/dt.jar:%JAVA_HOME%/lib/tools.jar:./bin/:./lib/*" gjh.bc5.main.BC5Runner

	If the tool cannot recognize any chemical-induced disease relation from the input file, the corresponding result file would contain none relation.


E. [References]
	Gu,J.H., Qian,L.H., and Zhou,G.D. (2015) Chemical-induced Disease Relation Extraction with Lexical Features. In Proceedings of the fifth BioCreative Challenge Evaluation Workshop, Sevilla, Spain, 220-225.
	Gu,J.H., Qian,L.H., and Zhou,G.D. (2016) Chemical-induced Disease Relation Extraction with Various Linguistic Features, Database (Oxford).(in review)
