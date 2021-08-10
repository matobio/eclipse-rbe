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

**Eclipse Install New Software**

1. Go to the menu "Help" in Eclipse. 
2. Choose "Install New Software".
3. Paste the next URL into the input "Work with:": ``https://raw.githubusercontent.com/matobio/eclipse-rbe/master/eclipse-rbe-update-site/site.xml``
4. Select the plugin version and click "Next".


**Manual Install:**

Download the plugin jar and copy it to Eclipse dropins directory (\eclipse\dropins\). The jar location is (replace ``<version>``): 
``https://github.com/matobio/eclipse-rbe/raw/master/eclipse-rbe-update-site/plugins/com.essiembre.eclipse.rbe_<version>.jar``.   Alternatively, you can download the entire Update Site bundled with everything else in the release link above and create a new update site in Eclipse, pointing to your local directory where you expanded the release.
