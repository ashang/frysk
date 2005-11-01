# This is used to find a file in a variety of paths, setting the value of the
# given variable to the the in which the file was found.
# AC_FIND_FILE ( file, dirs, variable )

AC_DEFUN([AC_GET_LIB_LINE],
[
	AC_FIND_FILE($1, $2, path)
	if [[ "x$path" == xno ]]; then
		$3 = $path
	else
		$3 = "-L$path -l$1"
	fi
]
)


