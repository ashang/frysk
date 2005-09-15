%define _prefix /opt
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc
%define sourcefile %{name_base}-%{version}.jar

%define installdir $RPM_BUILD_ROOT%{_datadir}/java

%define name_base cdtparser
Summary: C/C++ Parser from Eclipse CDT 3.0
Name: frysk-%{name_base}
Version: 3.0.0
Release: 1
Group: Parsers
License: EPL
Source0: %{sourcefile}
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root

Requires: java >= 1.4.2

%description
C/C++ Parser from the Eclipse CDT 3.0

%prep

%install
rm -rf $RPM_BUILD_ROOT
if ! test -d %{installdir};then
	mkdir -p %{installdir}
fi	
cp $RPM_SOURCE_DIR/%{sourcefile} %{installdir}


%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%attr(0644,root,bin) %{_datadir}/java/%{sourcefile}


%changelog
* Thu Sep 15 2005 Adam Jocksch <ajocksch@toothpaste.toronto.redhat.com> - 3.0.0-1
- Initial build.

