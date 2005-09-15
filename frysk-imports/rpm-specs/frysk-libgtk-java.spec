%define _prefix /opt
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc


%define	name_base 	libgtk-java
%define	version		2.8.0
%define	release		1

Summary:	Java bindings for GTK+
Name:		frysk-%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.gz
Patch0:		libgtk-java-2.8.0-treeModelrRootConstructor.patch
Patch1:		libgtk-java-2.8.0-supressBoxedDebugOutput.patch
Patch2:		libgtk-java-2.8.0-pkgConfigDependency.patch

BuildRoot:	%{_tmppath}/%{name_base}-%{version}-root

Requires:	frysk-gtk2 >= 2.8.0, frysk-cairo-java >= 1.0.0
BuildRequires:  java-devel >= 1.4.2
BuildRequires:  frysk-gtk2-devel >= 2.8.0, gcc-java >= 3.3.3, docbook-utils
ExclusiveArch:	i386 ppc s390

%description
libgtk-java is a language binding that allows developers to write GTK
applications in Java.  It is part of Java-GNOME.

This version of libgtk-java was specially packaged for use with the
frysk Execution Analysis Tool, it is not intended for general use.

%prep

%setup -q -n %{name_base}-%{version}
%patch0 -p0
%patch1 -p0
%patch2 -p0



%build 
export PKG_CONFIG_PATH=%{_libdir}/pkgconfig

%configure
make 

pushd doc
  mv FAQ.html ..
popd

%install
rm -rf %{buildroot}

make  DESTDIR=$RPM_BUILD_ROOT  install
rm $RPM_BUILD_ROOT%{_libdir}/*.la
rm -f $RPM_BUILD_ROOT%{_libdir}/*.a

# rename doc dir to reflect package rename
mv $RPM_BUILD_ROOT%{_docdir}/%{name_base}-%{version} $RPM_BUILD_ROOT%{_docdir}/%{name}-%{version}

%post
/sbin/ldconfig

%postun
/sbin/ldconfig

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root)
%doc doc/api AUTHORS COPYING INSTALL README NEWS  THANKS FAQ.html
%{_includedir}/*
%{_libdir}/*so*
%{_libdir}/pkgconfig/*
%{_datadir}/java/*.jar
%{_datadir}/%{name_base}
