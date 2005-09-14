%define _prefix /opt
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc

%define	name_base	cairo-java
%define	version		0.9.3
%define	release		2

Summary:	Java bindings for the Cairo library
Name:		frysk-%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.bz2
Patch0:		cairo-java-0.9.3-pkgConfigDependency.patch
BuildRoot:	%{_tmppath}/%{name}-%{version}-root

Requires:	frysk-gtk2 >= 2.8.0,frysk-cairo >= 0.9.2, frysk-jg-common >= 0.1
BuildRequires:  frysk-gtk2-devel >= 2.8.0, gcc-java >= 3.3.3, docbook-utils
ExclusiveArch:	i386 ppc s390

%description
Cairo-java is a language binding that allows developers to write Cairo
applications in Java.  It is part of Java-GNOME.

This version of cairo-java was specially packaged for use with the
frysk Execution Analysis Tool, it is not intended for general use.

%prep

%setup -q -n %{name_base}-%{version}
%patch0 -p0

# hack.  java-gnome should distribute the result of "make dist"
#ln -s autogen.sh configure
#mv autogen.sh configure

%build 
export PKG_CONFIG_PATH=%{_libdir}/pkgconfig

# hack, we need to generate the configure script, setting this vairable will
# make autogen not call ./configure after it's done
export AUTOGEN_SUBDIR_MODE=yes
./autogen.sh

# and now finally, configure
%configure
make

%install
rm -rf %{buildroot}

make  DESTDIR=$RPM_BUILD_ROOT install
rm $RPM_BUILD_ROOT%{_libdir}/*.la
rm -f $RPM_BUILD_ROOT%{_libdir}/*.a
# rename doc dir to reflect package rename
mv $RPM_BUILD_ROOT/opt/share/doc/%{name_base}-%{version} $RPM_BUILD_ROOT/opt/share/doc/%{name}-%{version}

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
%{_libdir}/pkgconfig/*
%{_datadir}/java/*

