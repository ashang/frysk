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

# The prefix for java-gnome package names
%define name_base glib-java
Summary:   Base Library for the Java-GNOME libraries 
Name:      %{java_pkg_prefix}%{name_base}
Version:   0.2.0
Release:   8
URL:       http://java-gnome.sourceforge.net
Source0:   %{name_base}-%{version}.tar.gz
License:   LGPL
Group:     Development/Libraries
BuildRoot: %{_tmppath}/glib-java


Requires: 	/sbin/ldconfig
Requires: 	%{c_pkg_prefix}glib2 >= 2.7.0
BuildRequires:  java-devel >= 1.4.2 %{c_pkg_prefix}glib2-devel >= 2.7.0
BuildRequires:  gcc-java >= 3.3.3, docbook-utils
BuildRequires: 	pkgconfig

%description 
Glib-java is a base framework for the Java-GNOME libraries. Allowing the use of
GNOME through Java.

%package        devel
Summary:        Compressed Java source files for %{name}.
Group:          Development/Libraries
Requires:       %{name} = %{version}-%{release}

%description    devel
Compressed Java source for %{name}. This is useful if you are developing
applications with IDEs like Eclipse.



%prep
rm -rf $RPM_BUILD_ROOT

%setup -q -n %{name_base}-%{version}


%build
# if either the C or Java packages has a prefix declared, then we will
# add /opt/frysk/lib/pkgconfig to the pkgconfig path
if  [  'x%{java_pkg_prefix}' != 'x' ] || [ 'x%{c_pkg_prefix}' != 'x' ]; then
	export PKG_CONFIG_PATH=/opt/frysk/%{lib}/pkgconfig
fi

%configure 

# FIXME: find a better solution for this
sed -i 's/^pic_flag=\"\"/pic_flag=\"\ \-fPIC\"/' libtool
sed -i 's/^compiler_c_o=\"no\"/compiler_c_o=\"yes\"/' libtool

make

# pack up the java source
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/^lib//")
zipfile=$PWD/$jarname$jarversion-src-%{version}.zip
pushd src/java
zip -9 -r $zipfile $(find -name \*.java)
popd


%install
rm -rf $RPM_BUILD_ROOT

make  DESTDIR=$RPM_BUILD_ROOT  install 

# rename doc dir to reflect package rename, if the names differ
if [ 'x%{name}' != 'x%{name_base}' ] ; then
	mv $RPM_BUILD_ROOT%{_docdir}/%{name_base}-%{version} $RPM_BUILD_ROOT%{_docdir}/%{name}-%{version}
fi

# install the src zip and make a sym link
jarversion=$(echo -n %{version} | cut -d . -f -2)
jarname=$(echo -n %{name_base} | cut -d - -f 1 | sed "s/^lib//")
install -m 644 $jarname$jarversion-src-%{version}.zip $RPM_BUILD_ROOT%{_datadir}/java/
pushd $RPM_BUILD_ROOT%{_datadir}/java
ln -sf $jarname$jarversion-src-%{version}.zip $jarname$jarversion-src.zip
popd


%clean
rm -rf $RPM_BUILD_ROOT

%post -p /sbin/ldconfig 
%postun -p /sbin/ldconfig

%files
%defattr(-,root,root,-)
%doc doc/api AUTHORS ChangeLog COPYING INSTALL NEWS README 
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
* Thu Oct 05 2005 Igor Foox <ifoox@redhat.com> - 0.2-8
- Imported released 0.2 version from upstream. 

* Thu Sep 29 2005 Igor Foox <ifoox@redhat.com> - 0.2-6
- Update sources, to reflect dependency on glib-java.

* Mon Sep 26 2005 Igor Foox <ifoox@redhat.com> - 0.2-5
- Changed optional installation prefix to /opt/frysk from opt.

* Mon Sep 19 2005 Igor Foox <ifoox@redhat.com> - 0.2-2
- Added to rawhide.
