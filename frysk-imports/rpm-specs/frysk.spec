#-----------------------------------------------------------------

# We're only building using frysk- packages now, specify this so we don't need
# to bother on the command line all the time.

%define java_pkg_prefix frysk-
%define c_pkg_prefix frysk-

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

# Architecture specific lib dir, lib64 on x86-64
%define base_libdir_name lib

%ifarch x86_64
%define lib %{base_libdir_name}64
%else
%ifarch x86
%define lib %{base_libdir_name}
%endif
%endif


%define	version		0.0	
%define	release		1

#-----------------------------------------------------------------

Summary:	Frysk execution analysis tool
Name:		frysk
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Debuggers
URL:		http://sourceware.org/frysk
Source:		%{name}-%{version}.tar.gz

BuildRoot:	%{_tmppath}/%{name}-%{version}-root

Requires:	%{java_pkg_prefix}libgtk-java >= 2.8.0
Requires:   %{java_pkg_prefix}libglade-java >= 2.12.0
Requires: 	%{java_pkg_prefix}libvte-java >= 0.11.11
BuildRequires:  antlr >= 2.7.4, junit >= 3.8.1, xfig >= 3.2.4, jdom >= 1.0
BuildRequires:  frysk-cdtparser >= 3.0.0
BuildRequires:  %{java_pkg_prefix}libgtk-java-devel >= 2.8.0
BuildRequires:	%{java_pkg_prefix}libglade-java >= 2.12.0
BuildRequires:	%{java_pkg_prefix}libvte-java-devel >= 0.11.11
BuildRequires:  java-devel >= 1.4.2, gcc-java >= 3.3.3, docbook-utils
BuildRequires: 	java-1.4.2-gcj-compat-devel

%description
Frysk is an execution-analysis technology implemented using native Java and C++.
It is aimed at providing developers and sysadmins with the ability to both
examine and analyze running multi-host, multi-process, multi-threaded systems.
Frysk allows the monitoring of running processes and threads, of locking
primitives and will also expose deadlocks, gather data and debug any given
process in the system.

%prep

%setup -q -n %{name}-%{version}


%build 
# For now we add /opt/frysk/lib/pkgconfig to the path to indicate that
# all the needed packages are installed there.
if  [  'x%{java_pkg_prefix}' != 'x' ] || [ 'x%{c_pkg_prefix}' != 'x' ]; then
	export PKG_CONFIG_PATH=/opt/frysk/%{lib}/pkgconfig
fi

%configure

make 

%install
rm -rf %{buildroot}

make  DESTDIR=$RPM_BUILD_ROOT  install



%post
/sbin/ldconfig

%postun
/sbin/ldconfig

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root)
%{_bindir}/ftrace
%{_bindir}/frysk
%{_libdir}/*so*
%dir %{_datadir}/%{name}
%{_datadir}/%{name}/*

%changelog
* Wed Oct 26  2005 Igor Foox <ifoox@redhat.com>
- Birth.
