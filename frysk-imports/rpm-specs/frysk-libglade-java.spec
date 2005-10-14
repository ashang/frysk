# install these packages into /opt if we have a prefix defined for the
# java packages
%{?java_pkg_prefix: %define _prefix /opt/frysk }
%{?java_pkg_prefix: %define _sysconfdir %{_prefix}/etc }
%{?java_pkg_prefix: %define _localstatedir %{_prefix}/var }
%{?java_pkg_prefix: %define _infodir %{_prefix}/share/info }
%{?java_pkg_prefix: %define _mandir %{_prefix}/share/man }
%{?java_pkg_prefix: %define _defaultdocdir %{_prefix}/share/doc }
%{?java_pkg_prefix: %define configure_args --without-gnome }

%{!?c_pkg_prefix: %define c_pkg_prefix %{nil}}
%{!?java_pkg_prefix: %define java_pkg_prefix %{nil}}

%define	name_base	libglade-java
%define	version		2.12.0
%define release		6

Summary:	Java bindings for libglade
Name:		%{java_pkg_prefix}%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.gz
BuildRoot:	%{_tmppath}/%{name}-%{version}-root

Requires:	libglade2 >= 2.5.0
Requires:	%{java_pkg_prefix}libgtk-java >= 2.8.0
BuildRequires:	%{java_pkg_prefix}libgtk-java >= 2.8.0
BuildRequires:  libglade2-devel >= 2.5.0, gcc-java >= 3.3.3
BuildRequires:	java-devel >= 1.4.2

%description
libglade-java is a language binding that allows developers to write
Java applications that use libglade.  It is part of Java-GNOME.

%package        devel
Summary:        Compressed Java source files for %{name}.
Group:          Development/Libraries
Requires:       %{name} = %{version}-%{release}

%description    devel
Compressed Java source for %{name}. This is useful if you are developing
applications with IDEs like Eclipse.


%prep
%setup -q -n %{name_base}-%{version}

%build
# if either the C or Java packages has a prefix declared, then we will
# add /opt/frysk/lib/pkgconfig to the pkgconfig path
if  [  'x%{java_pkg_prefix}' != 'x' ] || [ 'x%{c_pkg_prefix}' != 'x' ]; then
	export PKG_CONFIG_PATH=$PKG_CONFIG_PATH:/opt/frysk/lib/pkgconfig
fi

%configure %{configure_args}

mkdir -p doc/api/
make

# pack up the java source
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/lib//")
zipfile=$PWD/$jarname$jarversion-src-%{version}.zip
pushd src/java
zip -9 -r $zipfile $(find -name \*.java)
popd

%install
rm -rf %{buildroot}

make DESTDIR=$RPM_BUILD_ROOT install

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
%doc doc/api AUTHORS COPYING NEWS README
%{_libdir}/*so*
%{_libdir}/*la
%{_libdir}/pkgconfig/*
%{_datadir}/java/*.jar

%files devel
%defattr(-,root,root)
%{_datadir}/java/*.zip

%changelog
* Mon Oct 05 2005 Igor Foox <ifoox@redhat.com> - 2.12.0-6
- Imported released 2.12.0 sources from upstream.

* Mon Sep 26 2005 Igor Foox <ifoox@redhat.com> - 2.12.0-5
- Changed optional installation prefix to /opt/frysk from opt.

* Sat Sep 24 2005 Igor Foox <ifoox@redhat.com> - 2.12.0-4
- Imported libglade-java 2.12.0 from upstream.

* Fri May 20 2005 Ben Konrath <bkonrath@redhat.com> - 2.10.1-5
- Fix permissions of src zip.

* Thu May 19 2005 Ben Konrath <bkonrath@redhat.com> - 2.10.1-4
- Add compressed java source to devel package.

* Mon Apr 25 2005 Andrew Overholt <overholt@redhat.com> 2.10.1-3
- Back out patch added in 2.10.1-2 (unnecessary).

* Sat Apr 23 2005 Andrew Overholt <overholt@redhat.com> 2.10.1-2
- Add patch to fix NoSuchFieldError (Ismael Juma).

* Tue Apr 12 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.10.1-1
- Import libglade-java 2.10.1.

* Sat Apr  2 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.10.0-1
- Import libglade-java 2.10.0.

* Fri Mar  4 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.9.92-1
- Import libglade-java 2.9.92.

* Sat Feb 12 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.9.91.1-1
- Import libglade-java 2.9.91.1.

* Tue Feb  8 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.9.91-3
- Work around libtool, gcj, -D_FORTIFY_SOURCE=2, rpmbuild problem.

* Tue Feb  8 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.9.91-2
- Only build on i386 and x86_64.

* Tue Feb  8 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.9.91-1
- Import libglade-java 2.9.91.

* Fri Feb  4 2005 Thomas Fitzsimmons <fitzsim@redhat.com> - 2.9.90-1
- Import libglade-java 2.9.90.

* Mon Dec 13 2004 Ben Konrath <bkonrath@redhat.com> 2.8.2-2
- Add signal-connect.patch to fix upstream bug
  (http://bugzilla.gnome.org/show_bug.cgi?id=161190)

* Sat Nov 27 2004 Ben Konrath <bkonrath@redhat.com> 2.8.2-1
- Update sources

* Tue Nov  2 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.8.1-2
- Require libgnome-java and libgtk-java for build.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.8.1-1
- Initial release.

