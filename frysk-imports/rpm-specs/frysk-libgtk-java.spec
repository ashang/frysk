%define _prefix /opt
%define _sysconfdir %{_prefix}/etc
%define _localstatedir %{_prefix}/var
%define _infodir %{_prefix}/share/info
%define _mandir %{_prefix}/share/man
%define _defaultdocdir %{_prefix}/share/doc


%define	name_base 	libgtk-java
%define	version		2.7.0
%define	release		5

Summary:	Java bindings for GTK+
Name:		frysk-%{name_base}
Version:	%{version}
Release:	%{release}
License:	LGPL
Group:		Development/Libraries
URL:		http://java-gnome.sourceforge.net
Source:		%{name_base}-%{version}.tar.bz2
Patch0:		libgtk-java-2.7.0-datacolumnobject.patch	
Patch1:		libgtk-java-2.7.0-actionlisteners.patch
Patch2:  	libgtk-java-2.7.0-CustomEvents.patch 	
Patch3:		libgtk-java-2.7.0-treeModelrRootConstructor.patch
Patch4:		libgtk-java-2.7.0-supressBoxedDebugOutput.patch
BuildRoot:	%{_tmppath}/%{name_base}-%{version}-root

Requires:	frysk-gtk2 >= 2.8.0, frysk-cairo-java >= 0.9.2
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
%patch3 -p0
%patch4 -p0

# hack.  java-gnome should distribute the result of "make dist"
#ln -s autogen.sh configure

%build 
export PKG_CONFIG_PATH=%{_libdir}/pkgconfig

# hack, we need to generate the configure script, setting this vairable will
# make autogen not call ./configure after it's done
export AUTOGEN_SUBDIR_MODE=yes
./autogen.sh

# and now finally, configure
%configure
make 

#%{__make} %{?_smp_mflags}
pushd doc
#  docbook2html FAQ.sgml
#  perl -p -i -e 's/t1.html/FAQ.html/' t1.html
  mv FAQ.html ..
#  mv examples ..
#  rm -f FAQ.sgml .cvsignore
popd

%install
rm -rf %{buildroot}

make  DESTDIR=$RPM_BUILD_ROOT  install
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
%doc doc/api AUTHORS COPYING INSTALL README NEWS  THANKS FAQ.html
%{_includedir}/*
%{_libdir}/*so*
%{_libdir}/pkgconfig/*
%{_datadir}/java/*.jar
%{_datadir}/%{name_base}

%changelog
* Tue Sep 7 2005 Igor Foox <ifoox@redhat.com> 2.7.0-5
- Added patch for Boxed to supress un-needed debug output.

* Tue Sep 6 2005 Igor Foox <ifoox@redhat.com> 2.7.0-4
- Added patch for TreeModel constructor with a specified root.

* Wed Aug 31 2005 Igor Foox <ifoox@redhat.com> 2.7.0-3
- Added patch for CustomEvents to work.

* Wed Aug 31 2005 Igor Foox <ifoox@redhat.com> 2.7.0-2
- Added patch for ActionListeners to work.

* Sat Nov 27 2004 Ben Konrath <bkonrath@redhat.com> 2.4.6-1
- Update sources
- Remove libgtk-java-action-group.patch (fixed in upstream sources)

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-6
- Install examples directory by itself, not under doc.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-5
- Rewrite description.
- Fix location of run-example patch.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-4
- Build on ppc.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-3
- Don't build on ppc.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-2
- Bump release number.

* Mon Nov  1 2004 Thomas Fitzsimmons <fitzsim@redhat.com> 2.4.5-1
- Update to 2.4.5.
- Only build on 32-bit architectures.

* Sun Oct 17 2004 Luca De Rugeriis <piedamaro@fastwebnet.it> 2.4.3-2
- Few cleanups.
* Fri Oct 08 2004 Luca De Rugeriis <piedamaro@fastwebnet.it> 2.4.3-1
- Initial release.
