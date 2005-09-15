%define _prefix /opt
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc

%define name_base glib-java
Summary:   Base Library for the Java-GNOME libraries for the frysk debugger
Name:      frysk-%{name_base}
Version:   0.2
Release:   1
URL:       http://java-gnome.sourceforge.net
Source0:   %{name_base}-%{version}.tar.gz
License:   LGPL
Group:     System Environment/Libraries
BuildRoot: %{_tmppath}/frysk-glib-java


Requires: /sbin/ldconfig
Requires: frysk-glib2 >= 2.7.0
BuildRequires:  java-devel >= 1.4.2
BuildRequires:  gcc-java >= 3.3.3, docbook-utils
BuildRequires: pkgconfig

%description 
Glib-java is a base framework for the Java-GNOME libraries. Allowing the use of
GNOME through Java.

This version of glib-java  was specially packaged for use with the
frysk Execution Analysis Tool, it is not intended for general use.

%prep
rm -rf $RPM_BUILD_ROOT

%setup -q -n %{name_base}-%{version}


%build
export PKG_CONFIG_PATH=%{_libdir}/pkgconfig

%configure 
make

%install
rm -rf $RPM_BUILD_ROOT

make  DESTDIR=$RPM_BUILD_ROOT  install 
rm $RPM_BUILD_ROOT%{_libdir}/*.la
rm -f $RPM_BUILD_ROOT%{_libdir}/*.a

# rename doc dir to reflect package rename
mv $RPM_BUILD_ROOT%{_docdir}/%{name_base}-%{version} $RPM_BUILD_ROOT%{_docdir}/%{name}-%{version}

%clean
rm -rf $RPM_BUILD_ROOT

%post -p /sbin/ldconfig 
%postun -p /sbin/ldconfig

%files
%defattr(-,root,root,-)
%doc doc/api AUTHORS ChangeLog COPYING INSTALL NEWS README 
%{_includedir}/*
%{_libdir}/*so*
%{_libdir}/pkgconfig/*
%{_datadir}/java/*
%{_datadir}/%{name_base}
