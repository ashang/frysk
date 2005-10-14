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

%define	name_base	cairo-java
%define	version		1.0.0
%define	release		9


Summary:	Java bindings for the Cairo library
Name:		%{java_pkg_prefix}%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.gz
BuildRoot:	%{_tmppath}/%{name}-%{version}-root

Requires:	%{java_pkg_prefix}gtk2 >= 2.8.0
Requires: 	%{java_pkg_prefix}cairo >= 1.0.0 
Requires: 	%{java_pkg_prefix}glib-java >= 0.2
BuildRequires:	%{java_pkg_prefix}glib-java >= 0.2
BuildRequires:  %{c_pkg_prefix}gtk2-devel >= 2.8.0, gcc-java >= 3.3.3
BuildRequires:	docbook-utils

%description
Cairo-java is a language binding that allows developers to write Cairo
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

%build 
# if either the C or Java packages has a prefix declared, then we will
# add /opt/frysk/lib/pkgconfig to the pkgconfig path
if  [  'x%{java_pkg_prefix}' != 'x' ] || [ 'x%{c_pkg_prefix}' != 'x' ]; then
	export PKG_CONFIG_PATH=/opt/frysk/lib/pkgconfig
fi

%configure
make

# pack up the java source
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/^lib//")
zipfile=$PWD/$jarname$jarversion-src-%{version}.zip
pushd src/java
zip -9 -r $zipfile $(find -name \*.java)
popd


%install
rm -rf %{buildroot}

make  DESTDIR=$RPM_BUILD_ROOT install
rm -f $RPM_BUILD_ROOT%{_libdir}/*.a

# rename doc dir to reflect package rename, if the names differ
if [ 'x%{name_base}' != 'x%{name}' ] ; then
	mv $RPM_BUILD_ROOT%{_docdir}/%{name_base}-%{version} $RPM_BUILD_ROOT/%{_docdir}/%{name}-%{version}
fi

# install the src zip and make a sym link
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/^lib//")
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
%doc doc/api AUTHORS ChangeLog COPYING INSTALL README NEWS 
%{_libdir}/*so*
%{_libdir}/*la
%{_libdir}/pkgconfig/*
%{_datadir}/java/*

%files devel
%defattr(-,root,root)
%{_datadir}/java/*.zip

%changelog
* Mon Oct 05 2005 Igor Foox <ifoox@redhat.com> - 1.0.0-9
- Imported released 1.0.0 sources from upstream.

* Mon Sep 26 2005 Igor Foox <ifoox@redhat.com> - 1.0.0-8
- Changed optional installation prefix to /opt/frysk from opt.

* Thu Sep 22 2005 Igor Foox <ifoxo@redhat.com> - 1.0.0-6
- Made cairo-pdf background optional.

* Thu Sep 22 2005 Igor Foox <ifoxo@redhat.com> - 1.0.0-4
- Bumped Release number. Added glib-java to BuildRequires. Removed architecture restriction.

* Mon Sep 19 2005 Igor Foox <ifoox@redhat.com> - 1.0.0-3
- Added to rawhide.

