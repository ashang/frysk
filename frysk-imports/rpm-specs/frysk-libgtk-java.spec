# install these packages into /opt if we have a prefix defined for the
# java packages
%{?java_pkg_prefix: %define _prefix /opt/frysk }
%{?java_pkg_prefix: %define _sysconfdir %{_prefix}/etc }
%{?java_pkg_prefix: %define _localstatedir %{_prefix}/var }
%{?java_pkg_prefix: %define _infodir %{_prefix}/share/info }
%{?java_pkg_prefix: %define _mandir %{_prefix}/share/man }
%{?java_pkg_prefix: %define _defaultdocdir %{_prefix}/share/doc }

%{!?c_pkg_prefix: %define c_pkg_prefix %{nil}}
%{!?java_pkg_prefix: %define java_pkg_prefix %{nil}}

# Architecture specific lib dir
%define base_libdir_name lib

%ifarch x86_64
%define lib %{base_libdir_name}64
%else
%ifarch x86
%define lib %{base_libdir_name}
%endif
%endif



%define	name_base 	libgtk-java
%define	version		2.8.0
%define	release		13


Summary:	Java bindings for GTK+
Name:		%{java_pkg_prefix}%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.gz

BuildRoot:	%{_tmppath}/%{name_base}-%{version}-root

Requires:	%{c_pkg_prefix}gtk2 >= 2.8.0
Requires:	%{java_pkg_prefix}cairo-java >= 1.0.0
Requires:       %{java_pkg_prefix}glib-java >= 0.2
BuildRequires:  %{java_pkg_prefix}cairo-java >= 1.0.0
BuildRequires:  %{java_pkg_prefix}glib-java >= 0.2
BuildRequires:  %{c_pkg_prefix}gtk2-devel >= 2.8.0
BuildRequires:  java-devel >= 1.4.2, gcc-java >= 3.3.3, docbook-utils

%description
libgtk-java is a language binding that allows developers to write GTK
applications in Java.  It is part of Java-GNOME.

%package        devel
Summary:        Compressed Java source files for %{name}.
Group:          Development/Libraries
Requires:       %{name} = %{version}-%{release}

%description    devel
Compressed Java source for %{name}. This is useful if you are developing
applications with IDEs like Eclipse.

%prep

%setup -q -n %{name_base}-%{version}

# Patches for accelerators, remove when they get into upstream (2.8.1 ?)

%build 
# if either the C or Java packages has a prefix declared, then we will
# add /opt/frysk/lib/pkgconfig to the pkgconfig path
if  [  'x%{java_pkg_prefix}' != 'x' ] || [ 'x%{c_pkg_prefix}' != 'x' ]; then
	export PKG_CONFIG_PATH=/opt/frysk/%{lib}/pkgconfig
fi

%configure

make 

# pack up the java source
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/lib//")
zipfile=$PWD/$jarname$jarversion-src-%{version}.zip
pushd src/java
zip -9 -r $zipfile $(find -name \*.java)
popd

pushd doc
  mv FAQ.html ..
popd

%install
rm -rf %{buildroot}

make  DESTDIR=$RPM_BUILD_ROOT  install

# rename doc dir to reflect package rename, if the names differ
if [ 'x%{name_base}' != 'x%{name}' ] ; then
	mv $RPM_BUILD_ROOT%{_docdir}/%{name_base}-%{version} $RPM_BUILD_ROOT%{_docdir}/%{name}-%{version}
fi

# install the src zip and make a sym link
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/lib//")
install -m 644 $jarname$jarversion-src-%{version}.zip $RPM_BUILD_ROOT%{_datadir}/java/
pushd $RPM_BUILD_ROOT%{_datadir}/java
ln -sf $jarname$jarversion-src-%{version}.zip $jarname$jarversion-src.zip
popd


%post
/sbin/ldconfig

%postun
/sbin/ldconfig

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root)
%doc doc/api AUTHORS COPYING INSTALL README NEWS  THANKS FAQ.html doc/examples
%dir %{_includedir}/%{name_base}
%{_includedir}/%{name_base}/*
%{_libdir}/*so*
%{_libdir}/*la
%{_libdir}/pkgconfig/*
%{_datadir}/java/*.jar
%dir %{_datadir}/%{name_base}
%{_datadir}/%{name_base}/*

%files devel
%defattr(-,root,root)
%{_datadir}/java/*.zip


%changelog
* Tue Oct 18 2005  Igor Foox <ifoox@redhat.com> - 2.8.0-13
- Updated with patch for TextBuffer by ajocksch.

* Fri Oct 14 2005  Igor Foox <ifoox@redhat.com> - 2.8.0-11
- Updated sources to get bugfixes from upsteam.
- Removed Accelerators patch.

* Tue Oct 11 2005 Igor Foox <ifoox@redhat.com> - 2.8.0-10
- Fixed linking error due to accelerators patch.

* Fri Oct 07 2005 Igor Foox <ifoox@redhat.com> - 2.8.0-9
- Added new version of accelerators patch.

* Thu Oct 06 2005 Igor Foox <ifoox@redhat.com> - 2.8.0-8
- Imported released 2.8.0 sources from upstream.
- Added Adam Jocksch's patches for accelerators.

* Thu Sep 29 2005 Igor Foox <ifoox@redhat.com> - 2.8.0-7
- Updated sources, bugfixes.

* Mon Sep 26 2005 Igor Foox <ifoox@redhat.com> - 2.8.0-6
- Changed optional installation prefix to /opt/frysk from opt.

* Fri Sep 23 2005 Igor Foox <ifoox@redhat.com> - 2.8.0-5
- Updated to 2.8.0 upstream libgtk-java.

* Fri May 20 2005 Ben Konrath <bkonrath@redhat.com> - 2.6.2-3
- Fix permissions of src zip.

* Thu May 19 2005 Ben Konrath <bkonrath@redhat.com> - 2.6.2-2
- Add compressed java source to devel package.

* Mon Apr 11 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.6.2-1
- Import libgtk-java 2.6.2.

* Sat Apr  2 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.6.1.1-1
- Import libgtk-java 2.6.1.1.

* Wed Mar 16 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.6.1-3
- Import libgtk-java 2.6.1.

* Fri Mar  4 2005 Thomas Fitzsimmons <fitzsim@thor.perf.redhat.com> 2.6.0-3
- Add x86_64.

* Fri Mar  4 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.6.0-2
- Remove x86_64.
- Remove ppc64.
- Require java-devel for build, for javadoc.

* Wed Mar  2 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.6.0-1
- Import libgtk-java 2.6.0.
- Add ppc and ppc64.

* Sat Feb 12 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.91.1-1
- Import libgtk-java 2.5.91.1.

* Tue Feb  8 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.91-3
- Work around libtool, gcj, -D_FORTIFY_SOURCE=2, rpmbuild problem.

* Tue Feb  8 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.91-2
- Only build on i386 and x86_64.

* Tue Feb  8 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.91-1
- Import libgtk-java 2.5.91.

* Fri Feb  4 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.90-1
- Remove java-devel build requirement.
- Remove patch.
- Fix files section.

* Mon Jan 31 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.90-1
- Add patch to work around build failure.

* Thu Jan 13 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.5.90-1
- Import libgtk-java release 2.5.90.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-6
- Install examples directory by itself, not under doc.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-5
- Rewrite description.
- Fix location of run-example patch.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-4
- Build on ppc.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-3
- Don't build on ppc.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-2
- Bump release number.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-1
- Update to 2.4.5.
- Only build on 32-bit architectures.

* Sun Oct 17 2004 Luca De Rugeriis <piedamaro@fastwebnet.it> 2.4.3-2
- Few cleanups.
* Fri Oct 08 2004 Luca De Rugeriis <piedamaro@fastwebnet.it> 2.4.3-1
- Initial release.
                                                              164,1         Bo
