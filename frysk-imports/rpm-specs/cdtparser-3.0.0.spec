%define _prefix /opt/frysk
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc
%define sourcefile %{name_base}-%{version}.jar

%define installdir $RPM_BUILD_ROOT%{_datadir}/java
%define libdir $RPM_BUILD_ROOT%{_prefix}/lib

%define name_base cdtparser
Summary: C/C++ Parser from Eclipse CDT 3.0
Name: frysk-%{name_base}
Version: 3.0.0
Release: 4
Group: Parsers
License: EPL
Source0: %{sourcefile}
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root

BuildRequires: gcc-java >= 4.0.0.1
BuildRequires:  java-1.4.2-gcj-compat-devel >= 1.4.2.0-40jpp_18rh
Requires(post,postun): java-1.4.2-gcj-compat >= 1.4.2.0-40jpp_18rh
#BuildRequires:  java-devel >= 1.4.2

%description
C/C++ Parser from the Eclipse CDT 3.0

%prep
rm -fr $RPM_BUILD_DIR/%{name_base}
mkdir $RPM_BUILD_DIR/%{name_base}
cp $RPM_SOURCE_DIR/%{sourcefile} $RPM_BUILD_DIR/%{name_base}
cd $RPM_BUILD_DIR/%{name_base} && find-and-aot-compile %{name_base}-native "-fPIC -fjni"

%install
rm -rf $RPM_BUILD_ROOT
if ! test -d %{installdir};then
	mkdir -p %{installdir}
fi	

if ! test -d %{libdir}; then
	mkdir -p %{libdir}
fi

cp $RPM_BUILD_DIR/%{name_base}/%{name_base}-native/lib%{sourcefile}.so \
	%{libdir}/lib%{sourcefile}.so
ln %{libdir}/lib%{sourcefile}.so %{libdir}/lib%{name_base}.jar.so

cp $RPM_SOURCE_DIR/%{sourcefile} %{installdir}
ln %{installdir}/%{sourcefile} %{installdir}/%{name_base}.jar

rm -fr $RPM_BUILD_DIR/%{name-base}


%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%attr(0644,root,bin) %{_datadir}/java/%{sourcefile}
%attr(0664,root,bin) %{_datadir}/java/%{name_base}.jar
%attr(0644,root,bin) %{_prefix}/lib/lib%{sourcefile}.so
%attr(0664,root,bin) %{_prefix}/lib/lib%{name_base}.jar.so


%changelog
* Mon Sep 19 2005 Adam Jocksch <ajocksch@redhat.com> - 3.0.0-3
- Jar file now compiles to native so.
* Thu Sep 15 2005 Adam Jocksch <ajocksch@redhat.com> - 3.0.0-1
- Initial build.

