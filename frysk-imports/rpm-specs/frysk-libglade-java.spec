%define _prefix /opt
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc

%define configure_args --without-gnome

%define	name_base	libglade-java
%define	version		2.12.0
%define release		2

Summary:	Java bindings for libglade
Name:		frysk-%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.gz
Patch0:		libglade-java-2.12.0-pkgConfigDependency.patch
BuildRoot:	%{_tmppath}/%{name}-%{version}-root
Requires:	libglade2 >= 2.5.0
BuildRequires:  libglade2-devel >= 2.5.0, gcc-java >= 3.3.3
BuildRequires:	frysk-libgtk-java >= 2.8.0
BuildRequires:	java-devel >= 1.4.2
ExclusiveArch:	i386 ppc x86_64

%description
libglade-java is a language binding that allows developers to write
Java applications that use libglade.  It is part of Java-GNOME.

This version of libglade-java was specially packaged for use with the
frysk Execution Analysis Tool, it is not intended for general use.


%prep
%setup -q -n %{name_base}-%{version}
%patch0 -p0

%build
export PKG_CONFIG_PATH=%{_libdir}/pkgconfig

%configure %{configure_args}

mkdir -p doc/api/
make

%install
rm -rf %{buildroot}

make DESTDIR=$RPM_BUILD_ROOT install

rm -f %{buildroot}%{_libdir}/*la

mv $RPM_BUILD_ROOT/opt/share/doc/%{name_base}-%{version} $RPM_BUILD_ROOT/opt/share/doc/%{name}-%{version}

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
%{_libdir}/pkgconfig/*
%{_datadir}/java/*.jar

%changelog
* Mon Apr 25 2005 Matthew Hall <matt@nrpms.net> 2.10.1-1
- 2.10.1 Release
