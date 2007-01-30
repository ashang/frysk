#ifndef __FTK_CUSTOM_ATK_OBJECT_H__
#define __FTK_CUSTOM_ATK_OBJECT_H__

#include <gtk/gtk.h>
#include <gtk/gtkwidget.h>
#include <gtk/gtkbindings.h>
#include <glib.h>
#include <glib-object.h>

typedef struct _FtkCustomAtkObject
{
  AtkObject parent;

  int n_children;
  int start_index;

  //ftk_custom_s * custom;

  //AtkStateSet *state_set;

  // GtkWidget *widget;

  //GtkTextBuffer *text_buffer;

}
FtkCustomAtkObject;


typedef struct _FtkCustomAtkObjectClass
{
  AtkObjectClass parent_class;
}
FtkCustomAtkObjectClass;

AtkObject* ftk_custom_atk_object_new (GtkDrawingArea*);
void       ftk_custom_atk_object_init           (AtkObject*, gpointer data);
GType      ftk_custom_atk_object_get_type       (void);

gint       ftk_custom_atk_object_get_n_children (AtkObject *accessible);
AtkObject* ftk_custom_atk_object_ref_child      (AtkObject *accessible, gint     index);

void       ftk_custom_atk_object_set_n_children(FtkCustomAtkObject*,int n);
void       ftk_custom_atk_object_set_start_index(FtkCustomAtkObject*, int i);

#endif /* __FTK_CUSTOM_ATK_OBJECT_H__ */
