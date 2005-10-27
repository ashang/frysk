%define _prefix /opt/frysk
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc
%define sourcefile %{name_base}-%{version}.jar


# Architecture specific lib dir
%define base_libdir_name lib

%ifarch x86_64
%define lib %{base_libdir_name}64
%else
%ifarch x86
%define lib %{base_libdir_name}
%endif
%endif

%define lib lib

%define installdir $RPM_BUILD_ROOT%{_datadir}/java
%define libdir $RPM_BUILD_ROOT%{_libdir}

%define name_base cdtparser
Summary:	C/C++ Parser from Eclipse CDT 3.0
Name: 		frysk-%{name_base}
Version: 	3.0.0
Release: 	8
Group: 		Parsers
License: 	EPL
Source0: 	%{name_base}-%{version}.tar.gz
BuildRoot:	%{_tmppath}/%{name}-%{version}-%{release}-root

Requires: 				java >= 1.4.2
BuildRequires: 			gcc-java >= 4.0.0.1
BuildRequires:  		java-1.4.2-gcj-compat-devel >= 1.4.2.0-40jpp_18rh
Requires(post,postun):	java-1.4.2-gcj-compat >= 1.4.2.0-40jpp_18rh

%description
C/C++ Parser from the Eclipse CDT 3.0

%prep
%setup -q -n %{name_base}-%{version}

#cp $RPM_SOURCE_DIR/%{sourcefile} $RPM_BUILD_DIR/.

%build

# Create the .so file from the jar
gcj -fjni -fPIC -shared -o \
	lib%{sourcefile}.so %{sourcefile}

# Generate a .pc file from the pc.in
sed -e "s:@prefix@:%{_prefix}:g" -e "s:@exec_prefix@:%{_bindir}:g" \
	-e "s:@libdir@:%{_libdir}:g" -e "s:@includedir@:%{_includedir}:g" \
	-e "s:@VERSION@:%{version}:g" \
	-e "s:@INSTALLED_CLASSPATH@:%{_datadir}/java/%{name_base}.jar:g" \
	%{name_base}.pc.in > %{name_base}.pc

%install
rm -rf $RPM_BUILD_ROOT

# Create directories in the temporary install root
if ! test -d %{installdir};then
	mkdir -p %{installdir}
fi	

if ! test -d %{libdir}; then
	mkdir -p %{libdir}
fi

if ! test -d %{libdir}/pkgconfig; then
    mkdir -p %{libdir}/pkgconfig
fi


cp lib%{sourcefile}.so %{libdir}/
ln %{libdir}/lib%{sourcefile}.so %{libdir}/lib%{name_base}.jar.so

cp $RPM_SOURCE_DIR/%{sourcefile} %{installdir}
ln %{installdir}/%{sourcefile} %{installdir}/%{name_base}.jar

# Copy the pc.in file to the pkgconfig dir
cp %{name_base}.pc %{libdir}/pkgconfig/

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%{_datadir}/java/%{sourcefile}
%{_datadir}/java/%{name_base}.jar
%{_libdir}/lib%{sourcefile}.so
%{_libdir}/lib%{name_base}.jar.so
%{_libdir}/pkgconfig/*

%changelog
* Thu Oct 27 2005 Igor Foox <ifoox@redhat.com> 3.0.0-8
- Removed attribute setting for every file.
- Added tarball instead of a simple jar file.
- Added cdtparser.pc file, generated from .pc.in.

* Mon Oct 24 2005 Adam Jocksch <ajocksch@redhat.com>
- No longer use find-and-aot-compile to compile .jar to .so
- Now copy file properly to install dir.

* Fri Oct 21 2005 Igor Foox <ifoox@redhat.com> - 3.0.0-5
- Added architecture independent libdir definition (lib/lib64).

* Mon Sep 19 2005 Adam Jocksch <ajocksch@redhat.com> - 3.0.0-3
- Jar file now compiles to native so.

* Thu Sep 15 2005 Adam Jocksch <ajocksch@redhat.com> - 3.0.0-1
- Initial build.
