%define section free

%define name_base jdom


Name:           frysk-%{name_base}
Version:        1.0
Release:        1jpp_2fc
Epoch:          0
Summary:        Java alternative to DOM and SAX
License:        Apache Software License-like
URL:            http://www.jdom.org/
Group:          Development/Libraries/Java
Source0:        jdom-1.0-RHCLEAN.tar.bz2
Patch0:         %{name_base}-crosslink.patch
Requires:       xalan-j2 >= 0:2.2.0
BuildRequires:  jpackage-utils >= 0:1.5
BuildRequires:  java-javadoc
BuildRequires:  ant
BuildRequires:  xalan-j2 >= 0:2.2.0
BuildArch:      noarch
BuildRoot:      %{_tmppath}/%{name_base}-%{version}-%{release}-buildroot

%description
JDOM is, quite simply, a Java representation of an XML document. JDOM
provides a way to represent that document for easy and efficient
reading, manipulation, and writing. It has a straightforward API, is a
lightweight and fast, and is optimized for the Java programmer. It's an
alternative to DOM and SAX, although it integrates well with both DOM
and SAX.

%package javadoc
Summary:        Javadoc for %{name}
Group:          Development/Documentation

%description javadoc
Javadoc for %{name}.

%package demo
Summary:        Demos for %{name_base}
Group:          Development/Libraries/Java
Requires:       %{name} = %{epoch}:%{version}-%{release}

%description demo
Demonstrations and samples for %{name}.


%prep
%setup -q -n %{name_base}-%{version}
%patch0 -p0
# remove all binary libs
find . -name "*.jar" -exec rm -f {} \;
find . -name "*.class" -exec rm -f {} \;


%build
export CLASSPATH=$(build-classpath xalan-j2 xml-commons-apis)
sed -e 's|<property name="build.compiler".*||' build.xml > tempf; cp tempf build.xml; rm tempf
ant -Dj2se.apidoc=%{_javadocdir}/java package javadoc-link


%install
rm -rf $RPM_BUILD_ROOT

# jars
mkdir -p $RPM_BUILD_ROOT%{_javadir}
cp -p build/%{name_base}.jar $RPM_BUILD_ROOT%{_javadir}/%{name_base}-%{version}.jar
(cd $RPM_BUILD_ROOT%{_javadir} && for jar in *-%{version}.jar; do ln -sf ${jar} `echo $jar| sed "s|-%{version}||g"`; done)

# javadoc
mkdir -p $RPM_BUILD_ROOT%{_javadocdir}/%{name_base}-%{version}
cp -pr build/apidocs/* $RPM_BUILD_ROOT%{_javadocdir}/%{name_base}-%{version}
ln -s %{name_base}-%{version} $RPM_BUILD_ROOT%{_javadocdir}/%{name_base}

# demo
mkdir -p $RPM_BUILD_ROOT%{_datadir}/%{name_base}
cp -pr samples $RPM_BUILD_ROOT%{_datadir}/%{name_base}


%clean
rm -rf $RPM_BUILD_ROOT


%post javadoc
rm -f %{_javadocdir}/%{name_base}
ln -s %{name_base}-%{version} %{_javadocdir}/%{name_base}


%files
%defattr(0644,root,root,0755)
%doc CHANGES.txt COMMITTERS.txt LICENSE.txt README.txt TODO.txt
%{_javadir}/%{name_base}*.jar

%files javadoc
%defattr(0644,root,root,0755)
%ghost %doc %{_javadocdir}/%{name_base}
%doc %{_javadocdir}/%{name_base}-%{version}

%files demo
%defattr(0644,root,root,0755)
%{_datadir}/%{name_base}


%changelog
* Wed Jun 22 2005 Gary Benson <gbenson@redhat.com> - 0:1.0-1jpp_2fc
- Remove classes from the tarball too.

* Wed Jun 15 2005 Gary Benson <gbenson@redhat.com> - 0:1.0-1jpp_1fc
- Build into Fedora.

* Thu Jun  9 2005 Gary Benson <gbenson@redhat.com>
- Remove jarfiles from the tarball.

* Tue Oct 19 2004 Fernando Nasser <fnasser@redhat.com> - 0:1.0-1jpp_1rh
- First Red Hat build

* Sat Sep 18 2004 Ralph Apel <r.apel at r-apel.de> - 0:1.0-1jpp
- Upgrade to 1.0 final

* Tue Sep 07 2004 Ralph Apel <r.apel at r-apel.de> - 0:1.0-0.rc1.1jpp
- Upgrade to 1.0-rc1

* Sun Aug 23 2004 Randy Watler <rwatler at finali.com> - 0:1.0-0.b9.4jpp
- Rebuild with ant-1.6.2

* Mon Jul 19 2004 Ville Skyttä <ville.skytta at iki.fi> - 0:1.0-0.b9.3jpp
- Add non-versioned javadoc dir symlink.
- Crosslink with local J2SE javadocs.

* Thu Jan 22 2004 David Walluck <david@anti-microsoft.org> 0:1.0-0.b9.2jpp
- fix URL

* Wed Jan 21 2004 David Walluck <david@anti-microsoft.org> 0:1.0-0.b9.1jpp
- b9
- don't use classic compiler

* Thu Mar 27 2003 Ville Skyttä <ville.skytta at iki.fi> - 0:1.0-0.b8.2jpp
- Adapted to JPackage 1.5.
- Use sed instead of bash 2 extension when symlinking jars during build.

* Wed May 08 2002 Guillaume Rousse <guillomovitch@users.sourceforge.net> 1.0-0.b8.1jpp
- vendor, distribution, group tags

* Sat Jan 19 2002 Guillaume Rousse <guillomovitch@users.sourceforge.net> 1.0-0.b7.6jpp
- versioned dir for javadoc
- requires xalan-j2 >= 2.2.0
- no dependencies for javadoc package
- stricter dependency for demo package
- section macro

* Wed Dec 5 2001 Guillaume Rousse <guillomovitch@users.sourceforge.net> 1.0-0.b7.5jpp
- javadoc into javadoc package

* Wed Nov 21 2001 Christian Zoffoli <czoffoli@littlepenguin.org> 1.0-0.b7.4jpp
- removed packager tag
- new jpp extension
- added xalan 2.2.D13 support

* Sat Oct 6 2001 Guillaume Rousse <guillomovitch@users.sourceforge.net> 1.0-0.b7.3jpp
- used original tarball

* Sun Sep 30 2001 Guillaume Rousse <guillomovitch@users.sourceforge.net> 1.0-0.b7.2jpp
- first unified release
- s/jPackage/JPackage

* Mon Sep 17 2001 Guillaume Rousse <guillomovitch@users.sourceforge.net> 1.0-0.b7.1mdk
- Requires and BuildRequires xalan-j2
- vendor tag
- packager tag
- s/Copyright/License/
- truncated description to 72 columns in spec
- spec cleanup
- used versioned jar
- added demo package

*  Sat Feb 17 2001 Guillaume Rousse <g.rousse@linux-mandrake.com> 1.0b6-1mdk
- first Mandrake release
