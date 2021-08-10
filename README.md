ResourceBundle Editor
===========

Eclipse plugin for editing Java resource bundles. Lets you manage all localized .properties files in one screen. Some features: sorted keys, warning icons on missing keys/values, conversion to/from Unicode, hierarchical view of keys, and more.


<img src="http://matobio.github.io/eclipse-rbe/img/screenshots/main_1_0_7.png">

Go to ResourceBundle Editor web site for more screenshots and other information: http://matobio.github.io/eclipse-rbe/


Changes
--------------

- New translate button on each textbox field to translate the text using an API Rest Translator.

How to install
--------------

**Update Site:**

Create a new update site in Eclipse with the following:

* Site name:  ``ResourceBundle Editor``
* Site URL:   ``https://raw.githubusercontent.com/essiembre/eclipse-rbe/master/eclipse-rbe-update-site/site.xml``


**Manual Install:**

Download the plugin jar and copy it to Eclipse dropins directory. The jar location is (replace ``<version>``): 
``https://raw.githubusercontent.com/essiembre/eclipse-rbe/master/eclipse-rbe-update-site/plugins/com.essiembre.eclipse.rbe_<version>.jar``.   Alternatively, you can download the entire Update Site bundled with everything else in the release link above and create a new update site in Eclipse, pointing to your local directory where you expanded the release.
