;;
;;  french.nsh
;;
;;  French language strings for the Windows JOSM NSIS installer.
;;  Windows Code page: 1252
;;
;;  Author: Vincent Privat <vprivat@openstreetmap.fr>, 2011.
;;

; Make sure to update the JOSM_MACRO_LANGUAGEFILE_END macro in
; langmacros.nsh when updating this file

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_WELCOME_TEXT "Cet assistant va vous guider � travers l'installation de l'�diteur Java OpenStreetMap (JOSM).$\r$\n$\r$\nAvant de lancer l'installation, assurez-vous que JOSM n'est pas d�j� en cours d'ex�cution.$\r$\n$\r$\nVeuillez cliquer sur 'Suivant' pour continuer."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_DIR_TEXT "Veuillez choisir un dossier o� installer JOSM."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_FULL_INSTALL "JOSM (installation compl�te)"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_JOSM "JOSM"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_PLUGINS_GROUP "Greffons"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_IMAGERY_OFFSET_DB_PLUGIN  "ImageryOffsetDatabase"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_TURNRESTRICTIONS_PLUGIN  "TurnRestrictions"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_STARTMENU  "Entr�e dans le menu D�marrer"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_DESKTOP_ICON  "Ic�ne sur le Bureau"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_QUICKLAUNCH_ICON  "Ic�ne dans la barre de lancement rapide"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_FILE_EXTENSIONS  "Extensions de fichier"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_JOSM "JOSM est l'�diteur Java OpenStreetMap pour les fichiers .osm."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_PLUGINS_GROUP "Une s�lection de greffons utiles pour JOSM."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_IMAGERY_OFFSET_DB_PLUGIN  "Base de donn�es de d�calages d'imagerie: partager et acqu�rir des d�calages d'imagerie avec un seul bouton."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_TURNRESTRICTIONS_PLUGIN  "Permet de saisir et de maintenir des informations sur les restrictions de tourner."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_STARTMENU  "Ajoute une entr�e JOSM au menu d�marrer."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_DESKTOP_ICON  "Ajoute une ic�ne JOSM au Bureau."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_QUICKLAUNCH_ICON  "Ajoute une ic�ne JOSM � la barre de lancement rapide."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_FILE_EXTENSIONS  "Associe JOSM aux extensions de fichier .osm et .gpx."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR1 "La biblioth�que 'shell32.dll' est introuvable. Impossible de mettre � jour les ic�nes"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR2 "Vous devriez installer le compl�ment gratuit 'Microsoft Layer for Unicode' pour mettre � jour les fichiers d'ic�nes de JOSM"

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_LINK_TEXT "�diteur Java OpenStreetMap"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_UNCONFIRMPAGE_TEXT_TOP "L'installation suivante de l'�diteur Java OpenStreetMap (JOSM) va �tre d�sinstall�e. Veuillez cliquer sur 'Suivant' pour continuer."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_DEFAULT_UNINSTALL "D�faut (conserve les param�tres personnels et les greffons)"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_FULL_UNINSTALL "Tout (supprime l'int�gralit� des fichiers)"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_IN_USE_ERROR "Attention: JOSM n'a pas pu �tre retir�, il est probablement en utilisation !"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_INSTDIR_ERROR "Attention: Le dossier $INSTDIR n'a pas pu �tre supprim� !"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_UNINSTALL "JOSM" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PERSONAL_SETTINGS "Param�tres personnels" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PLUGINS "Greffons personnels" 

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_UNINSTALL "D�sinstaller JOSM."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_PERSONAL_SETTINGS  "D�sinstaller les param�tres personnels de votre profil: $PROFILE."
